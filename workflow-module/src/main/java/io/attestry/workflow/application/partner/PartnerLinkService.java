package io.attestry.workflow.application.partner;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.partner.command.CreatePartnerLinkCommand;
import io.attestry.workflow.application.partner.result.PartnerLinkResult;
import io.attestry.workflow.application.port.common.TenantReadPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.PartnerLinkUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.repository.PartnerLinkRepository;
import io.attestry.workflow.domain.partner.policy.PartnerLinkCreatePolicy;
import io.attestry.workflow.domain.partner.policy.PartnerLinkCreatePolicy.PartnerLinkCreateContext;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PartnerLinkService implements PartnerLinkUseCase {

    private final PartnerLinkRepository repository;
    private final TenantReadPort tenantReadPort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final PartnerLinkCreatePolicy createPolicy;
    private final Clock clock;


    @Override
    @Transactional
    public PartnerLinkResult create(AuthPrincipal principal, CreatePartnerLinkCommand command) {
        String sourceTenantId = requireTenantId(principal);
        authorizationSupport.assertTenantContext(principal, sourceTenantId);
        authorizationSupport.assertLivePermission(principal, sourceTenantId, PermissionCodes.PARTNER_LINK_CREATE,
                "partner-link:create");
        Instant now = Instant.now(clock);
        boolean sourceTenantActive = tenantReadPort.existsActiveTenant(sourceTenantId);
        boolean targetTenantActive = tenantReadPort.existsActiveTenant(command.targetTenantId());
        boolean alreadyActive = repository.existsBySourceAndTargetAndTypeAndStatus(
            sourceTenantId,
            command.targetTenantId(),
            command.partnerType(),
            PartnerLinkStatus.ACTIVE
        );
        createPolicy.assertCreatable(new PartnerLinkCreateContext(
            sourceTenantId,
            command.targetTenantId(),
            sourceTenantActive,
            targetTenantActive,
            alreadyActive,
            command.proposedExpiresAt(),
            now
        ));
        PartnerLink created = repository.save(PartnerLink.create(
            sourceTenantId,
            command.targetTenantId(),
            command.partnerType(),
            principal.userId(),
            command.proposedExpiresAt(),
            now
        ));
        return toResult(created, loadSourceSummary(sourceTenantId), loadTargetTenantNames(List.of(created)));
    }

    @Override
    @Transactional
    public PartnerLinkResult approve(AuthPrincipal principal, String partnerLinkId) {
        PartnerLink partnerLink = getById(partnerLinkId);
        return saveTransition(
            principal,
            partnerLink,
            PermissionCodes.PARTNER_LINK_APPROVE,
            link -> link.approve(principal.userId(), Instant.now(clock))
        );
    }

    @Override
    @Transactional
    public PartnerLinkResult reject(AuthPrincipal principal, String partnerLinkId, String reason) {
        PartnerLink partnerLink = getById(partnerLinkId);
        return saveTransition(
            principal,
            partnerLink,
            PermissionCodes.PARTNER_LINK_APPROVE,
            link -> link.reject(principal.userId(), reason, Instant.now(clock))
        );
    }

    @Override
    @Transactional
    public PartnerLinkResult suspend(AuthPrincipal principal, String partnerLinkId) {
        PartnerLink partnerLink = getById(partnerLinkId);
        return saveTransition(
            principal,
            partnerLink,
            PermissionCodes.PARTNER_LINK_SUSPEND,
            link -> link.suspend(principal.userId(), Instant.now(clock))
        );
    }

    @Override
    @Transactional
    public PartnerLinkResult resume(AuthPrincipal principal, String partnerLinkId) {
        PartnerLink partnerLink = getById(partnerLinkId);
        return saveTransition(
            principal,
            partnerLink,
            PermissionCodes.PARTNER_LINK_RESUME,
            link -> link.resume(principal.userId(), Instant.now(clock))
        );
    }

    @Override
    @Transactional
    public PartnerLinkResult terminate(AuthPrincipal principal, String partnerLinkId, String reason) {
        PartnerLink partnerLink = getById(partnerLinkId);
        return saveTransition(
            principal,
            partnerLink,
            PermissionCodes.PARTNER_LINK_TERMINATE,
            link -> link.terminate(principal.userId(), reason, Instant.now(clock))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerLinkResult> listByTenant(AuthPrincipal principal, PartnerLinkStatus status) {
        String tenantId = requireTenantId(principal);
        authorizationSupport.assertTenantContext(principal, tenantId);
        List<PartnerLink> links = repository.findByTenantId(tenantId, status);
        TenantReadPort.TenantSummary sourceSummary = loadSourceSummary(tenantId);
        Map<String, String> targetTenantNames = loadTargetTenantNames(links);
        return links.stream().map(link -> toResult(link, sourceSummary, targetTenantNames)).toList();
    }

    private String requireTenantId(AuthPrincipal principal) {
        if (principal.tenantId() == null || principal.tenantId().isBlank()) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_REQUEST,
                "Tenant-scoped token is required"
            );
        }
        return principal.tenantId();
    }

    private PartnerLink getById(String partnerLinkId) {
        return repository.findById(partnerLinkId)
                .orElseThrow(() -> new io.attestry.workflow.domain.WorkflowDomainException(
                    io.attestry.workflow.domain.WorkflowErrorCode.PARTNER_LINK_NOT_FOUND,
                    "Partner link not found"
                ));
    }

    private PartnerLinkResult saveTransition(
        AuthPrincipal principal,
        PartnerLink partnerLink,
        String permissionCode,
        java.util.function.Function<PartnerLink, PartnerLink> transition
    ) {
        assertSourceTenantAccess(principal, partnerLink, permissionCode);
        PartnerLink saved = repository.save(transition.apply(partnerLink));
        return toResult(saved, loadSourceSummary(saved.sourceTenantId()), loadTargetTenantNames(List.of(saved)));
    }

    private void assertSourceTenantAccess(AuthPrincipal principal, PartnerLink partnerLink, String permissionCode) {
        authorizationSupport.assertTenantContext(principal, partnerLink.sourceTenantId());
        authorizationSupport.assertLivePermission(
            principal,
            partnerLink.sourceTenantId(),
            permissionCode,
            "partner-link:" + partnerLink.partnerLinkId()
        );
    }

    private TenantReadPort.TenantSummary loadSourceSummary(String tenantId) {
        return tenantReadPort.findTenantSummary(tenantId);
    }

    private Map<String, String> loadTargetTenantNames(List<PartnerLink> links) {
        return tenantReadPort.findTenantNamesByIds(
            links.stream()
                .map(PartnerLink::targetTenantId)
                .distinct()
                .toList()
        );
    }

    private PartnerLinkResult toResult(
        PartnerLink link,
        TenantReadPort.TenantSummary source,
        Map<String, String> targetTenantNames
    ) {
        return new PartnerLinkResult(
            link.partnerLinkId(),
            link.sourceTenantId(),
            source != null ? source.name() : null,
            source != null ? source.type() : null,
            link.targetTenantId(),
            targetTenantNames.get(link.targetTenantId()),
            link.partnerType().name(),
            link.status().name(),
            link.reason(),
            link.expiresAt()
        );
    }
}

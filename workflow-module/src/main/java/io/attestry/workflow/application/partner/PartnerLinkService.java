package io.attestry.workflow.application.partner;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.partner.command.CreatePartnerLinkCommand;
import io.attestry.workflow.application.partner.result.PartnerLinkResult;
import io.attestry.workflow.application.partner.result.TenantSearchResult;
import io.attestry.workflow.application.port.TenantReadPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.PartnerLinkUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.repository.PartnerLinkRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerLinkService implements PartnerLinkUseCase {

    private final PartnerLinkRepository repository;
    private final TenantReadPort tenantReadPort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final Clock clock;

    public PartnerLinkService(
            PartnerLinkRepository repository,
            TenantReadPort tenantReadPort,
            WorkflowAuthorizationSupport authorizationSupport,
            Clock clock) {
        this.repository = repository;
        this.tenantReadPort = tenantReadPort;
        this.authorizationSupport = authorizationSupport;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PartnerLinkResult create(AuthPrincipal principal, CreatePartnerLinkCommand command) {
        String sourceTenantId = requireTenantId(principal);
        authorizationSupport.assertTenantContext(principal, sourceTenantId);
        authorizationSupport.assertLivePermission(principal, sourceTenantId, PermissionCodes.PARTNER_LINK_CREATE,
                "partner-link:create");
        if (!tenantReadPort.existsActiveTenant(sourceTenantId)
                || !tenantReadPort.existsActiveTenant(command.targetTenantId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Tenant not found or inactive");
        }
        if (sourceTenantId.equals(command.targetTenantId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST,
                    "Target tenant must be different from source tenant");
        }
        boolean alreadyActive = repository.existsBySourceAndTargetAndTypeAndStatus(
                sourceTenantId,
                command.targetTenantId(),
                command.partnerType(),
                PartnerLinkStatus.ACTIVE);
        if (alreadyActive) {
            throw new WorkflowDomainException(WorkflowErrorCode.PARTNER_LINK_ALREADY_ACTIVE,
                    "Active partner link already exists");
        }
        PartnerLink created = repository.save(PartnerLink.create(
                sourceTenantId,
                command.targetTenantId(),
                command.partnerType(),
                principal.userId(),
                command.proposedExpiresAt(),
                Instant.now(clock)));
        return toResult(created);
    }

    @Override
    @Transactional
    public PartnerLinkResult approve(AuthPrincipal principal, String partnerLinkId) {
        PartnerLink partnerLink = getById(partnerLinkId);
        String policyTenantId = principal.tenantId() != null ? principal.tenantId() : partnerLink.sourceTenantId();
        authorizationSupport.assertLivePermission(principal, policyTenantId, PermissionCodes.PARTNER_LINK_APPROVE,
                "partner-link:" + partnerLinkId);
        return toResult(repository.save(partnerLink.approve(principal.userId(), Instant.now(clock))));
    }

    @Override
    @Transactional
    public PartnerLinkResult reject(AuthPrincipal principal, String partnerLinkId, String reason) {
        PartnerLink partnerLink = getById(partnerLinkId);
        String policyTenantId = principal.tenantId() != null ? principal.tenantId() : partnerLink.sourceTenantId();
        authorizationSupport.assertLivePermission(principal, policyTenantId, PermissionCodes.PARTNER_LINK_APPROVE,
                "partner-link:" + partnerLinkId);
        return toResult(repository.save(partnerLink.reject(principal.userId(), reason, Instant.now(clock))));
    }

    @Override
    @Transactional
    public PartnerLinkResult suspend(AuthPrincipal principal, String partnerLinkId) {
        PartnerLink partnerLink = getById(partnerLinkId);
        authorizationSupport.assertTenantContext(principal, partnerLink.sourceTenantId());
        authorizationSupport.assertLivePermission(principal, partnerLink.sourceTenantId(),
                PermissionCodes.PARTNER_LINK_SUSPEND, "partner-link:" + partnerLinkId);
        return toResult(repository.save(partnerLink.suspend(principal.userId(), Instant.now(clock))));
    }

    @Override
    @Transactional
    public PartnerLinkResult resume(AuthPrincipal principal, String partnerLinkId) {
        PartnerLink partnerLink = getById(partnerLinkId);
        authorizationSupport.assertTenantContext(principal, partnerLink.sourceTenantId());
        authorizationSupport.assertLivePermission(principal, partnerLink.sourceTenantId(),
                PermissionCodes.PARTNER_LINK_RESUME, "partner-link:" + partnerLinkId);
        return toResult(repository.save(partnerLink.resume(principal.userId(), Instant.now(clock))));
    }

    @Override
    @Transactional
    public PartnerLinkResult terminate(AuthPrincipal principal, String partnerLinkId, String reason) {
        PartnerLink partnerLink = getById(partnerLinkId);
        authorizationSupport.assertTenantContext(principal, partnerLink.sourceTenantId());
        authorizationSupport.assertLivePermission(principal, partnerLink.sourceTenantId(),
                PermissionCodes.PARTNER_LINK_TERMINATE, "partner-link:" + partnerLinkId);
        return toResult(repository.save(partnerLink.terminate(principal.userId(), reason, Instant.now(clock))));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerLinkResult> listByTenant(AuthPrincipal principal, PartnerLinkStatus status) {
        String tenantId = requireTenantId(principal);
        authorizationSupport.assertTenantContext(principal, tenantId);
        List<PartnerLink> links = status == null
                ? repository.findByTenantId(tenantId)
                : repository.findByTenantIdAndStatus(tenantId, status);
        return links.stream().map(this::toResult).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantSearchResult> searchActiveTenantsByName(AuthPrincipal principal, String name) {
        String tenantId = requireTenantId(principal);
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.PARTNER_LINK_READ,
                "partner-link:tenant-search");

        if (name == null || name.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "name is required");
        }

        return tenantReadPort.searchActiveTenantsByName(name.trim()).stream()
                .map(tenant -> new TenantSearchResult(tenant.tenantId(), tenant.name(), tenant.region(), tenant.type()))
                .toList();
    }

    private String requireTenantId(AuthPrincipal principal) {
        if (principal.tenantId() == null || principal.tenantId().isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Tenant-scoped token is required");
        }
        return principal.tenantId();
    }

    private PartnerLink getById(String partnerLinkId) {
        return repository.findById(partnerLinkId)
                .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.PARTNER_LINK_NOT_FOUND,
                        "Partner link not found"));
    }

    private PartnerLinkResult toResult(PartnerLink link) {
        String sourceTenantName = tenantReadPort.findTenantName(link.sourceTenantId());
        String targetTenantName = tenantReadPort.findTenantName(link.targetTenantId());
        return new PartnerLinkResult(
                link.partnerLinkId(),
                link.sourceTenantId(),
                sourceTenantName,
                link.targetTenantId(),
                targetTenantName,
                link.partnerType().name(),
                link.status().name(),
                link.reason(),
                link.expiresAt());
    }
}

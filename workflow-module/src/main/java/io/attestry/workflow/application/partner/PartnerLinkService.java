package io.attestry.workflow.application.partner;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.auth.model.PermissionCodes;
import io.attestry.workflow.application.partner.command.CreatePartnerLinkCommand;
import io.attestry.workflow.application.partner.result.PartnerLinkResult;
import io.attestry.workflow.application.port.PartnerLinkRepositoryPort;
import io.attestry.workflow.application.port.TenantReadPort;
import io.attestry.workflow.application.usecase.PartnerLinkUseCase;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerLinkService implements PartnerLinkUseCase {

    private final PartnerLinkRepositoryPort repository;
    private final TenantReadPort tenantReadPort;
    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;
    private final Clock clock;

    public PartnerLinkService(
        PartnerLinkRepositoryPort repository,
        TenantReadPort tenantReadPort,
        EvaluateAuthorizationUseCase evaluateAuthorizationUseCase,
        Clock clock
    ) {
        this.repository = repository;
        this.tenantReadPort = tenantReadPort;
        this.evaluateAuthorizationUseCase = evaluateAuthorizationUseCase;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PartnerLinkResult create(AuthPrincipal principal, String brandTenantId, CreatePartnerLinkCommand command) {
        assertTenantContext(principal, brandTenantId);
        assertLivePermission(principal, brandTenantId, PermissionCodes.PARTNER_LINK_CREATE, "partner-link:create");
        if (!tenantReadPort.existsActiveTenant(brandTenantId) || !tenantReadPort.existsActiveTenant(command.partnerTenantId())) {
            throw new DomainException(ErrorCode.TENANT_NOT_FOUND, "Tenant not found or inactive");
        }
        if (brandTenantId.equals(command.partnerTenantId())) {
            throw new DomainException(ErrorCode.INVALID_APPLICATION_STATE, "Partner tenant must be different from brand tenant");
        }
        boolean alreadyActive = repository.existsByBrandAndPartnerAndTypeAndStatus(
            brandTenantId,
            command.partnerTenantId(),
            command.partnerType(),
            PartnerLinkStatus.ACTIVE
        );
        if (alreadyActive) {
            throw new DomainException(ErrorCode.PARTNER_LINK_ALREADY_ACTIVE, "Active partner link already exists");
        }
        PartnerLink created = repository.save(PartnerLink.create(
            brandTenantId,
            command.partnerTenantId(),
            command.partnerType(),
            principal.userId(),
            Instant.now(clock)
        ));
        return toResult(created);
    }

    @Override
    @Transactional
    public PartnerLinkResult approve(AuthPrincipal principal, String partnerLinkId) {
        PartnerLink partnerLink = getById(partnerLinkId);
        String policyTenantId = principal.tenantId() != null ? principal.tenantId() : partnerLink.brandTenantId();
        assertLivePermission(principal, policyTenantId, PermissionCodes.PARTNER_LINK_APPROVE, "partner-link:" + partnerLinkId);
        return toResult(repository.save(partnerLink.approve(principal.userId(), Instant.now(clock))));
    }

    @Override
    @Transactional
    public PartnerLinkResult reject(AuthPrincipal principal, String partnerLinkId, String reason) {
        PartnerLink partnerLink = getById(partnerLinkId);
        String policyTenantId = principal.tenantId() != null ? principal.tenantId() : partnerLink.brandTenantId();
        assertLivePermission(principal, policyTenantId, PermissionCodes.PARTNER_LINK_APPROVE, "partner-link:" + partnerLinkId);
        return toResult(repository.save(partnerLink.reject(principal.userId(), reason, Instant.now(clock))));
    }

    @Override
    @Transactional
    public PartnerLinkResult suspend(AuthPrincipal principal, String partnerLinkId) {
        PartnerLink partnerLink = getById(partnerLinkId);
        assertTenantContext(principal, partnerLink.brandTenantId());
        assertLivePermission(principal, partnerLink.brandTenantId(), PermissionCodes.PARTNER_LINK_SUSPEND, "partner-link:" + partnerLinkId);
        return toResult(repository.save(partnerLink.suspend(principal.userId(), Instant.now(clock))));
    }

    @Override
    @Transactional
    public PartnerLinkResult resume(AuthPrincipal principal, String partnerLinkId) {
        PartnerLink partnerLink = getById(partnerLinkId);
        assertTenantContext(principal, partnerLink.brandTenantId());
        assertLivePermission(principal, partnerLink.brandTenantId(), PermissionCodes.PARTNER_LINK_RESUME, "partner-link:" + partnerLinkId);
        return toResult(repository.save(partnerLink.resume(principal.userId(), Instant.now(clock))));
    }

    @Override
    @Transactional
    public PartnerLinkResult terminate(AuthPrincipal principal, String partnerLinkId, String reason) {
        PartnerLink partnerLink = getById(partnerLinkId);
        assertTenantContext(principal, partnerLink.brandTenantId());
        assertLivePermission(principal, partnerLink.brandTenantId(), PermissionCodes.PARTNER_LINK_TERMINATE, "partner-link:" + partnerLinkId);
        return toResult(repository.save(partnerLink.terminate(principal.userId(), reason, Instant.now(clock))));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerLinkResult> listByTenant(AuthPrincipal principal, String tenantId) {
        assertTenantContext(principal, tenantId);
        return repository.findByTenantId(tenantId).stream().map(this::toResult).toList();
    }

    private PartnerLink getById(String partnerLinkId) {
        return repository.findById(partnerLinkId)
            .orElseThrow(() -> new DomainException(ErrorCode.PARTNER_LINK_NOT_FOUND, "Partner link not found"));
    }

    private void assertTenantContext(AuthPrincipal principal, String tenantId) {
        if (principal.tenantId() == null || !principal.tenantId().equals(tenantId)) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant access denied");
        }
    }

    private void assertLivePermission(AuthPrincipal principal, String tenantId, String action, String resourceRef) {
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            principal,
            new AuthzEvaluateCommand(tenantId, action, resourceRef, PolicyDecisionMode.LIVE_RECHECK)
        );
        if (!decision.allowed()) {
            throw new DomainException(
                decision.reason() != null ? ErrorCode.valueOf(decision.reason()) : ErrorCode.FORBIDDEN_SCOPE,
                "Action denied by live policy check"
            );
        }
    }

    private PartnerLinkResult toResult(PartnerLink link) {
        return new PartnerLinkResult(
            link.partnerLinkId(),
            link.brandTenantId(),
            link.partnerTenantId(),
            link.partnerType().name(),
            link.status().name(),
            link.reason()
        );
    }
}

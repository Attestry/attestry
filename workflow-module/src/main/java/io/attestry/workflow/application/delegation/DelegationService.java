package io.attestry.workflow.application.delegation;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.auth.model.PermissionCodes;
import io.attestry.workflow.application.delegation.command.GrantDelegationCommand;
import io.attestry.workflow.application.delegation.result.DelegationEvaluateResult;
import io.attestry.workflow.application.delegation.result.DelegationResult;
import io.attestry.workflow.application.port.DelegationRepositoryPort;
import io.attestry.workflow.application.port.PartnerLinkRepositoryPort;
import io.attestry.workflow.application.port.TenantReadPort;
import io.attestry.workflow.application.usecase.DelegationUseCase;
import io.attestry.workflow.domain.delegation.model.Delegation;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DelegationService implements DelegationUseCase {

    private final DelegationRepositoryPort delegationRepository;
    private final PartnerLinkRepositoryPort partnerLinkRepository;
    private final TenantReadPort tenantReadPort;
    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;
    private final Clock clock;

    public DelegationService(
        DelegationRepositoryPort delegationRepository,
        PartnerLinkRepositoryPort partnerLinkRepository,
        TenantReadPort tenantReadPort,
        EvaluateAuthorizationUseCase evaluateAuthorizationUseCase,
        Clock clock
    ) {
        this.delegationRepository = delegationRepository;
        this.partnerLinkRepository = partnerLinkRepository;
        this.tenantReadPort = tenantReadPort;
        this.evaluateAuthorizationUseCase = evaluateAuthorizationUseCase;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DelegationResult grant(AuthPrincipal principal, String brandTenantId, GrantDelegationCommand command) {
        assertTenantContext(principal, brandTenantId);
        assertLivePermission(principal, brandTenantId, PermissionCodes.DELEGATION_GRANT, "delegation:grant");
        PartnerLink partnerLink = partnerLinkRepository.findById(command.partnerLinkId())
            .orElseThrow(() -> new DomainException(ErrorCode.PARTNER_LINK_NOT_FOUND, "Partner link not found"));

        if (partnerLink.status() != PartnerLinkStatus.ACTIVE) {
            throw new DomainException(ErrorCode.PARTNER_LINK_INVALID_STATE, "Partner link must be active");
        }
        if (!partnerLink.brandTenantId().equals(brandTenantId) || !partnerLink.partnerTenantId().equals(command.partnerTenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Partner link tenant mismatch");
        }
        if (!tenantReadPort.existsActiveTenant(brandTenantId) || !tenantReadPort.existsActiveTenant(command.partnerTenantId())) {
            throw new DomainException(ErrorCode.TENANT_NOT_FOUND, "Tenant not found or inactive");
        }
        if (delegationRepository.existsActive(
            brandTenantId,
            command.partnerTenantId(),
            command.resourceType(),
            command.resourceId(),
            command.permissionCode()
        )) {
            throw new DomainException(ErrorCode.DELEGATION_ALREADY_ACTIVE, "Active delegation already exists");
        }

        Delegation granted = delegationRepository.save(Delegation.grant(
            command.partnerLinkId(),
            brandTenantId,
            command.partnerTenantId(),
            command.resourceType(),
            command.resourceId(),
            command.permissionCode(),
            command.expiresAt(),
            principal.userId(),
            Instant.now(clock),
            command.note()
        ));
        return toResult(granted);
    }

    @Override
    @Transactional
    public DelegationResult revoke(AuthPrincipal principal, String delegationId, String reason) {
        Delegation delegation = delegationRepository.findById(delegationId)
            .orElseThrow(() -> new DomainException(ErrorCode.DELEGATION_NOT_FOUND, "Delegation not found"));
        assertTenantContext(principal, delegation.brandTenantId());
        assertLivePermission(principal, delegation.brandTenantId(), PermissionCodes.DELEGATION_REVOKE, "delegation:" + delegationId);
        Delegation revoked = delegationRepository.save(delegation.revoke(principal.userId(), reason, Instant.now(clock)));
        return toResult(revoked);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DelegationResult> listByTenant(AuthPrincipal principal, String tenantId) {
        assertTenantContext(principal, tenantId);
        return delegationRepository.findByTenantId(tenantId).stream().map(this::toResult).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DelegationEvaluateResult evaluate(
        String brandTenantId,
        String partnerTenantId,
        String resourceType,
        String resourceId,
        String permissionCode
    ) {
        Delegation delegation = delegationRepository.findActive(
                brandTenantId,
                partnerTenantId,
                resourceType,
                resourceId,
                permissionCode
            )
            .orElse(null);

        if (delegation == null) {
            return new DelegationEvaluateResult(false, "DELEGATION_NOT_FOUND");
        }
        if (delegation.isExpired(Instant.now(clock))) {
            return new DelegationEvaluateResult(false, "DELEGATION_EXPIRED");
        }
        PartnerLink link = partnerLinkRepository.findById(delegation.partnerLinkId()).orElse(null);
        if (link == null || link.status() != PartnerLinkStatus.ACTIVE) {
            return new DelegationEvaluateResult(false, "PARTNER_LINK_NOT_ACTIVE");
        }
        return new DelegationEvaluateResult(true, null);
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

    private DelegationResult toResult(Delegation delegation) {
        return new DelegationResult(
            delegation.delegationId(),
            delegation.partnerLinkId(),
            delegation.brandTenantId(),
            delegation.partnerTenantId(),
            delegation.resourceType(),
            delegation.resourceId(),
            delegation.permissionCode(),
            delegation.status().name(),
            delegation.expiresAt(),
            delegation.reason()
        );
    }
}

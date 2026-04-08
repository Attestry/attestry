package io.attestry.workflow.application.delegation.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.port.delegation.PassportAuthorityQueryPort;
import io.attestry.workflow.application.delegation.result.DelegationResult;
import io.attestry.workflow.application.delegation.support.RelationshipValidator;
import io.attestry.workflow.application.port.delegation.DelegationPermissionProjectionPort;
import io.attestry.workflow.application.port.common.TenantReadPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.delegation.model.Delegation;
import io.attestry.workflow.domain.delegation.policy.DelegationGrantPolicy;
import io.attestry.workflow.domain.delegation.repository.DelegationRepository;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.model.PartnerType;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class DelegationService implements DelegationUseCase {

    private final DelegationRepository delegationRepository;
    private final TenantReadPort tenantReadPort;
    private final PassportAuthorityQueryPort passportAuthorityQueryPort;
    private final DelegationPermissionProjectionPort permissionProjectionPort;
    private final RelationshipValidator relationshipValidator;
    private final DelegationGrantPolicy delegationGrantPolicy;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final Clock clock;


    @Override
    @Transactional
    public DelegationResult grant(WorkflowActorContext principal, String sourceTenantId, GrantDelegationCommand command) {
        authorizationSupport.assertTenantContext(principal, sourceTenantId);
        authorizationSupport.assertLivePermission(principal, sourceTenantId, PermissionCodes.DELEGATION_GRANT, "delegation:grant");

        PartnerLink partnerLink = relationshipValidator.assertEligibleBySource(
            command.partnerLinkId(), sourceTenantId
        );
        assertPartnerTypeAllowed(command, partnerLink);
        String targetTenantId = partnerLink.targetTenantId();
        assertTenantsActive(sourceTenantId, targetTenantId);

        Instant now = Instant.now(clock);
        DelegationGrantPolicy.DelegationGrantContext context = resolveGrantContext(sourceTenantId, targetTenantId, command, now);
        delegationGrantPolicy.assertGrantable(context);

        Delegation granted = delegationRepository.save(Delegation.grant(
            command.partnerLinkId(), sourceTenantId, targetTenantId,
            command.resourceType(), command.resourceId(), command.permissionCode(),
            command.expiresAt(), principal.userId(), now, command.note()
        ));

        if (granted.isPassportPermissionGrant()) {
            permissionProjectionPort.onDelegationGranted(granted, partnerLink.status().name());
        }
        return toResult(granted);
    }

    private void assertTenantsActive(String sourceTenantId, String targetTenantId) {
        if (!tenantReadPort.existsActiveTenant(sourceTenantId) || !tenantReadPort.existsActiveTenant(targetTenantId)) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Tenant not found or inactive");
        }
    }

    private void assertPartnerTypeAllowed(GrantDelegationCommand command, PartnerLink partnerLink) {
        if (
            "RETAIL_TRANSFER_CREATE".equals(command.permissionCode())
                && partnerLink.partnerType() != PartnerType.RETAIL
        ) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.PARTNER_LINK_INVALID_TYPE,
                "해당 파트너는 유통(판매) 권한이 없는 서비스 타입 업체입니다."
            );
        }
    }

    private DelegationGrantPolicy.DelegationGrantContext resolveGrantContext(
        String sourceTenantId, String targetTenantId, GrantDelegationCommand command, Instant now
    ) {
        Optional<PassportAuthorityQueryPort.PassportAuthorityRecord> passport =
            "PASSPORT".equals(command.resourceType())
                ? passportAuthorityQueryPort.findPassportAuthority(command.resourceId())
                : Optional.empty();

        boolean activeDelegationExists = delegationRepository.existsActive(
            sourceTenantId, targetTenantId,
            command.resourceType(), command.resourceId(), command.permissionCode()
        );

        return new DelegationGrantPolicy.DelegationGrantContext(
            sourceTenantId,
            command.resourceType(),
            command.permissionCode(),
            command.expiresAt(),
            passport.map(PassportAuthorityQueryPort.PassportAuthorityRecord::tenantId).orElse(null),
            passport.map(PassportAuthorityQueryPort.PassportAuthorityRecord::assetState).orElse(null),
            activeDelegationExists,
            now
        );
    }

    @Override
    @Transactional
    public DelegationResult revoke(WorkflowActorContext principal, String delegationId, String reason) {
        Delegation delegation = delegationRepository.findById(delegationId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.DELEGATION_NOT_FOUND, "Delegation not found"));
        authorizationSupport.assertTenantContext(principal, delegation.sourceTenantId());
        authorizationSupport.assertLivePermission(principal, delegation.sourceTenantId(), PermissionCodes.DELEGATION_REVOKE, "delegation:" + delegationId);
        Delegation revoked = delegationRepository.save(delegation.revoke(principal.userId(), reason, Instant.now(clock)));
        if (revoked.isPassportPermissionGrant()) {
            permissionProjectionPort.onDelegationRevoked(revoked);
        }
        return toResult(revoked);
    }

    private DelegationResult toResult(Delegation delegation) {
        return new DelegationResult(
            delegation.delegationId(),
            delegation.partnerLinkId(),
            delegation.sourceTenantId(),
            delegation.targetTenantId(),
            delegation.resourceType(),
            delegation.resourceId(),
            delegation.permissionCode(),
            delegation.status().name(),
            delegation.expiresAt(),
            delegation.reason()
        );
    }
}

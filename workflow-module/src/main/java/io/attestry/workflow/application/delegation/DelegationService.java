package io.attestry.workflow.application.delegation;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.delegation.command.BatchGrantPassportDelegationCommand;
import io.attestry.workflow.application.delegation.command.GrantDelegationCommand;
import io.attestry.workflow.application.port.PassportAuthorityQueryPort;
import io.attestry.workflow.application.delegation.result.BatchDelegationResult;
import io.attestry.workflow.application.delegation.result.DelegationResult;
import io.attestry.workflow.application.port.DelegationPermissionProjectionPort;
import io.attestry.workflow.application.port.TenantReadPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.DelegationUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.delegation.model.Delegation;
import io.attestry.workflow.domain.delegation.policy.DelegationGrantPolicy;
import io.attestry.workflow.domain.delegation.repository.DelegationRepository;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public DelegationService(
        DelegationRepository delegationRepository,
        TenantReadPort tenantReadPort,
        PassportAuthorityQueryPort passportAuthorityQueryPort,
        DelegationPermissionProjectionPort permissionProjectionPort,
        RelationshipValidator relationshipValidator,
        DelegationGrantPolicy delegationGrantPolicy,
        WorkflowAuthorizationSupport authorizationSupport,
        Clock clock
    ) {
        this.delegationRepository = delegationRepository;
        this.tenantReadPort = tenantReadPort;
        this.passportAuthorityQueryPort = passportAuthorityQueryPort;
        this.permissionProjectionPort = permissionProjectionPort;
        this.relationshipValidator = relationshipValidator;
        this.delegationGrantPolicy = delegationGrantPolicy;
        this.authorizationSupport = authorizationSupport;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DelegationResult grant(AuthPrincipal principal, String sourceTenantId, GrantDelegationCommand command) {
        authorizationSupport.assertTenantContext(principal, sourceTenantId);
        authorizationSupport.assertLivePermission(principal, sourceTenantId, PermissionCodes.DELEGATION_GRANT, "delegation:grant");

        PartnerLink partnerLink = relationshipValidator.assertEligible(
            command.partnerLinkId(), sourceTenantId, command.targetTenantId()
        );
        assertTenantsActive(sourceTenantId, command.targetTenantId());

        Instant now = Instant.now(clock);
        DelegationGrantPolicy.DelegationGrantContext context = resolveGrantContext(sourceTenantId, command, now);
        delegationGrantPolicy.assertGrantable(context);

        Delegation granted = delegationRepository.save(Delegation.grant(
            command.partnerLinkId(), sourceTenantId, command.targetTenantId(),
            command.resourceType(), command.resourceId(), command.permissionCode(),
            command.expiresAt(), principal.userId(), now, command.note()
        ));

        if (granted.isPassportPermissionGrant()) {
            permissionProjectionPort.onDelegationGranted(granted, partnerLink.status().name());
        }
        return toResult(granted);
    }

    @Override
    @Transactional
    public BatchDelegationResult batchGrantPassportDelegation(
        AuthPrincipal principal,
        String sourceTenantId,
        String partnerLinkId,
        BatchGrantPassportDelegationCommand command
    ) {
        authorizationSupport.assertTenantContext(principal, sourceTenantId);
        authorizationSupport.assertLivePermission(principal, sourceTenantId, PermissionCodes.DELEGATION_GRANT, "delegation:batch-grant");

        PartnerLink partnerLink = relationshipValidator.assertEligibleBySource(partnerLinkId, sourceTenantId);
        String targetTenantId = partnerLink.targetTenantId();
        assertTenantsActive(sourceTenantId, targetTenantId);

        Instant now = Instant.now(clock);
        List<BatchDelegationResult.Entry> results = new ArrayList<>();

        for (String passportId : command.passportIds()) {
            results.add(grantSinglePassport(
                partnerLinkId, sourceTenantId, targetTenantId, passportId,
                command.expiresAt(), command.note(), principal.userId(), partnerLink, now
            ));
        }

        long granted = results.stream().filter(BatchDelegationResult.Entry::isGranted).count();
        return new BatchDelegationResult(results, command.passportIds().size(), granted);
    }

    private BatchDelegationResult.Entry grantSinglePassport(
        String partnerLinkId, String sourceTenantId, String targetTenantId,
        String passportId, Instant expiresAt, String note,
        String actorUserId, PartnerLink partnerLink, Instant now
    ) {
        try {
            DelegationGrantPolicy.DelegationGrantContext context = resolveGrantContextForPassport(
                sourceTenantId, targetTenantId, passportId, expiresAt, now
            );
            delegationGrantPolicy.assertGrantable(context);

            Delegation granted = delegationRepository.save(Delegation.grant(
                partnerLinkId, sourceTenantId, targetTenantId,
                "PASSPORT", passportId, "RETAIL_TRANSFER_CREATE",
                expiresAt, actorUserId, now, note
            ));

            if (granted.isPassportPermissionGrant()) {
                permissionProjectionPort.onDelegationGranted(granted, partnerLink.status().name());
            }
            return BatchDelegationResult.Entry.granted(passportId, granted.delegationId());
        } catch (WorkflowDomainException ex) {
            return BatchDelegationResult.Entry.failed(passportId, ex.getErrorCode().name());
        }
    }

    private DelegationGrantPolicy.DelegationGrantContext resolveGrantContextForPassport(
        String sourceTenantId, String targetTenantId,
        String passportId, Instant expiresAt, Instant now
    ) {
        Optional<PassportAuthorityQueryPort.PassportAuthorityView> passport =
            passportAuthorityQueryPort.findPassportAuthority(passportId);

        boolean activeDelegationExists = delegationRepository.existsActive(
            sourceTenantId, targetTenantId, "PASSPORT", passportId, "RETAIL_TRANSFER_CREATE"
        );

        return new DelegationGrantPolicy.DelegationGrantContext(
            sourceTenantId,
            "PASSPORT",
            "RETAIL_TRANSFER_CREATE",
            expiresAt,
            passport.map(PassportAuthorityQueryPort.PassportAuthorityView::tenantId).orElse(null),
            passport.map(PassportAuthorityQueryPort.PassportAuthorityView::assetState).orElse(null),
            activeDelegationExists,
            now
        );
    }

    private void assertTenantsActive(String sourceTenantId, String targetTenantId) {
        if (!tenantReadPort.existsActiveTenant(sourceTenantId) || !tenantReadPort.existsActiveTenant(targetTenantId)) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Tenant not found or inactive");
        }
    }

    private DelegationGrantPolicy.DelegationGrantContext resolveGrantContext(
        String sourceTenantId, GrantDelegationCommand command, Instant now
    ) {
        Optional<PassportAuthorityQueryPort.PassportAuthorityView> passport =
            "PASSPORT".equals(command.resourceType())
                ? passportAuthorityQueryPort.findPassportAuthority(command.resourceId())
                : Optional.empty();

        boolean activeDelegationExists = delegationRepository.existsActive(
            sourceTenantId, command.targetTenantId(),
            command.resourceType(), command.resourceId(), command.permissionCode()
        );

        return new DelegationGrantPolicy.DelegationGrantContext(
            sourceTenantId,
            command.resourceType(),
            command.permissionCode(),
            command.expiresAt(),
            passport.map(PassportAuthorityQueryPort.PassportAuthorityView::tenantId).orElse(null),
            passport.map(PassportAuthorityQueryPort.PassportAuthorityView::assetState).orElse(null),
            activeDelegationExists,
            now
        );
    }

    @Override
    @Transactional
    public DelegationResult revoke(AuthPrincipal principal, String delegationId, String reason) {
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

    @Override
    @Transactional(readOnly = true)
    public List<DelegationResult> listByTenant(AuthPrincipal principal, String tenantId) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        return delegationRepository.findByTenantId(tenantId).stream().map(this::toResult).toList();
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

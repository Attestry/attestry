package io.attestry.workflow.application.delegation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.auth.model.VerificationLevel;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.delegation.command.DelegationService;
import io.attestry.workflow.application.delegation.internal.RelationshipValidator;
import io.attestry.workflow.application.delegation.command.GrantDelegationCommand;
import io.attestry.workflow.application.delegation.command.DelegationResult;
import io.attestry.workflow.application.port.common.TenantReadPort;
import io.attestry.workflow.application.port.delegation.DelegationPermissionProjectionPort;
import io.attestry.workflow.application.port.delegation.PassportAuthorityQueryPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.delegation.model.Delegation;
import io.attestry.workflow.domain.delegation.policy.DelegationGrantPolicy;
import io.attestry.workflow.domain.delegation.repository.DelegationRepository;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.model.PartnerType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DelegationServiceTest {

    @Mock DelegationRepository delegationRepository;
    @Mock TenantReadPort tenantReadPort;
    @Mock PassportAuthorityQueryPort passportAuthorityQueryPort;
    @Mock DelegationPermissionProjectionPort permissionProjectionPort;
    @Mock RelationshipValidator relationshipValidator;
    @Mock WorkflowAuthorizationSupport authorizationSupport;

    private final DelegationGrantPolicy delegationGrantPolicy = new DelegationGrantPolicy();
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-12T00:00:00Z"), ZoneOffset.UTC);

    private DelegationService service;

    private static final WorkflowActorContext PRINCIPAL = new WorkflowActorContext(
        "token-1",
        "user-1",
        "source-tenant",
        VerificationLevel.PHONE_VERIFIED,
        Set.of("SCOPE_DELEGATION_GRANT", "SCOPE_DELEGATION_REVOKE"),
        Instant.parse("2026-03-13T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new DelegationService(
            delegationRepository,
            tenantReadPort,
            passportAuthorityQueryPort,
            permissionProjectionPort,
            relationshipValidator,
            delegationGrantPolicy,
            authorizationSupport,
            clock
        );
    }

    @Test
    void grant_savesDelegationAndSyncsPermissionProjection() {
        GrantDelegationCommand command = new GrantDelegationCommand(
            "pl-1",
            "PASSPORT",
            "passport-1",
            "RETAIL_TRANSFER_CREATE",
            Instant.parse("2026-04-01T00:00:00Z"),
            "note"
        );
        PartnerLink link = new PartnerLink(
            "pl-1",
            "source-tenant",
            "target-tenant",
            PartnerType.RETAIL,
            PartnerLinkStatus.ACTIVE,
            "user-1",
            Instant.parse("2026-03-01T00:00:00Z"),
            "approver-1",
            Instant.parse("2026-03-02T00:00:00Z"),
            null,
            null,
            null
        );

        doNothing().when(authorizationSupport).assertTenantContext(PRINCIPAL, "source-tenant");
        doNothing().when(authorizationSupport).assertLivePermission(
            PRINCIPAL, "source-tenant", PermissionCodes.DELEGATION_GRANT, "delegation:grant"
        );
        when(relationshipValidator.assertEligibleBySource("pl-1", "source-tenant")).thenReturn(link);
        when(tenantReadPort.existsActiveTenant("source-tenant")).thenReturn(true);
        when(tenantReadPort.existsActiveTenant("target-tenant")).thenReturn(true);
        when(passportAuthorityQueryPort.findPassportAuthority("passport-1"))
            .thenReturn(Optional.of(new PassportAuthorityQueryPort.PassportAuthorityRecord(
                "passport-1",
                "source-tenant",
                "ACTIVE",
                "NONE"
            )));
        when(delegationRepository.existsActive("source-tenant", "target-tenant", "PASSPORT", "passport-1", "RETAIL_TRANSFER_CREATE"))
            .thenReturn(false);
        when(delegationRepository.save(any(Delegation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DelegationResult result = service.grant(PRINCIPAL, "source-tenant", command);

        assertEquals("source-tenant", result.sourceTenantId());
        assertEquals("target-tenant", result.targetTenantId());
        assertEquals("ACTIVE", result.status());
        verify(permissionProjectionPort).onDelegationGranted(any(Delegation.class), org.mockito.Mockito.eq("ACTIVE"));
    }

    @Test
    void grant_rejectsDuplicateActiveDelegation() {
        GrantDelegationCommand command = new GrantDelegationCommand(
            "pl-1",
            "PASSPORT",
            "passport-1",
            "RETAIL_TRANSFER_CREATE",
            Instant.parse("2026-04-01T00:00:00Z"),
            "note"
        );
        PartnerLink link = new PartnerLink(
            "pl-1",
            "source-tenant",
            "target-tenant",
            PartnerType.RETAIL,
            PartnerLinkStatus.ACTIVE,
            "user-1",
            Instant.parse("2026-03-01T00:00:00Z"),
            "approver-1",
            Instant.parse("2026-03-02T00:00:00Z"),
            null,
            null,
            null
        );

        doNothing().when(authorizationSupport).assertTenantContext(PRINCIPAL, "source-tenant");
        doNothing().when(authorizationSupport).assertLivePermission(
            PRINCIPAL, "source-tenant", PermissionCodes.DELEGATION_GRANT, "delegation:grant"
        );
        when(relationshipValidator.assertEligibleBySource("pl-1", "source-tenant")).thenReturn(link);
        when(tenantReadPort.existsActiveTenant("source-tenant")).thenReturn(true);
        when(tenantReadPort.existsActiveTenant("target-tenant")).thenReturn(true);
        when(passportAuthorityQueryPort.findPassportAuthority("passport-1"))
            .thenReturn(Optional.of(new PassportAuthorityQueryPort.PassportAuthorityRecord(
                "passport-1",
                "source-tenant",
                "ACTIVE",
                "NONE"
            )));
        when(delegationRepository.existsActive("source-tenant", "target-tenant", "PASSPORT", "passport-1", "RETAIL_TRANSFER_CREATE"))
            .thenReturn(true);

        WorkflowDomainException ex = assertThrows(
            WorkflowDomainException.class,
            () -> service.grant(PRINCIPAL, "source-tenant", command)
        );

        assertEquals(WorkflowErrorCode.DELEGATION_ALREADY_ACTIVE, ex.getErrorCode());
        verify(delegationRepository, never()).save(any());
        verify(permissionProjectionPort, never()).onDelegationGranted(any(), any());
    }

    @Test
    void grant_rejectsNonRetailPartnerForRetailTransferPermission() {
        GrantDelegationCommand command = new GrantDelegationCommand(
            "pl-1",
            "PASSPORT",
            "passport-1",
            "RETAIL_TRANSFER_CREATE",
            Instant.parse("2026-04-01T00:00:00Z"),
            "note"
        );
        PartnerLink link = new PartnerLink(
            "pl-1",
            "source-tenant",
            "target-tenant",
            PartnerType.SERVICE,
            PartnerLinkStatus.ACTIVE,
            "user-1",
            Instant.parse("2026-03-01T00:00:00Z"),
            "approver-1",
            Instant.parse("2026-03-02T00:00:00Z"),
            null,
            null,
            null
        );

        doNothing().when(authorizationSupport).assertTenantContext(PRINCIPAL, "source-tenant");
        doNothing().when(authorizationSupport).assertLivePermission(
            PRINCIPAL, "source-tenant", PermissionCodes.DELEGATION_GRANT, "delegation:grant"
        );
        when(relationshipValidator.assertEligibleBySource("pl-1", "source-tenant")).thenReturn(link);

        WorkflowDomainException ex = assertThrows(
            WorkflowDomainException.class,
            () -> service.grant(PRINCIPAL, "source-tenant", command)
        );

        assertEquals(WorkflowErrorCode.PARTNER_LINK_INVALID_TYPE, ex.getErrorCode());
        verify(delegationRepository, never()).save(any());
        verify(permissionProjectionPort, never()).onDelegationGranted(any(), any());
    }

    @Test
    void revoke_savesDelegationAndSyncsProjection() {
        Delegation active = Delegation.grant(
            "pl-1",
            "source-tenant",
            "target-tenant",
            "PASSPORT",
            "passport-1",
            "RETAIL_TRANSFER_CREATE",
            Instant.parse("2026-04-01T00:00:00Z"),
            "user-1",
            Instant.parse("2026-03-12T00:00:00Z"),
            "note"
        );

        doNothing().when(authorizationSupport).assertTenantContext(PRINCIPAL, "source-tenant");
        doNothing().when(authorizationSupport).assertLivePermission(
            PRINCIPAL, "source-tenant", PermissionCodes.DELEGATION_REVOKE, "delegation:" + active.delegationId()
        );
        when(delegationRepository.findById(active.delegationId())).thenReturn(Optional.of(active));
        when(delegationRepository.save(any(Delegation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DelegationResult result = service.revoke(PRINCIPAL, active.delegationId(), "reason");

        assertEquals("REVOKED", result.status());
        verify(permissionProjectionPort).onDelegationRevoked(any(Delegation.class));
    }
}

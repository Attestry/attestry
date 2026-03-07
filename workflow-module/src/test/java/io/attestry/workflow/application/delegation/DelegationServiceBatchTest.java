package io.attestry.workflow.application.delegation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.identity.model.VerificationLevel;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.delegation.command.BatchGrantPassportDelegationCommand;
import io.attestry.workflow.application.delegation.result.BatchDelegationResult;
import io.attestry.workflow.application.port.DelegationPermissionProjectionPort;
import io.attestry.workflow.application.port.PassportAuthorityQueryPort;
import io.attestry.workflow.application.port.PassportAuthorityQueryPort.PassportAuthorityView;
import io.attestry.workflow.application.port.TenantReadPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.delegation.model.Delegation;
import io.attestry.workflow.domain.delegation.policy.DelegationGrantPolicy;
import io.attestry.workflow.domain.delegation.repository.DelegationRepository;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.model.PartnerType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DelegationServiceBatchTest {

    @Mock DelegationRepository delegationRepository;
    @Mock TenantReadPort tenantReadPort;
    @Mock PassportAuthorityQueryPort passportAuthorityQueryPort;
    @Mock DelegationPermissionProjectionPort permissionProjectionPort;
    @Mock RelationshipValidator relationshipValidator;
    @Mock WorkflowAuthorizationSupport authorizationSupport;

    private final DelegationGrantPolicy delegationGrantPolicy = new DelegationGrantPolicy();
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    private DelegationService service;

    private static final String SOURCE_TENANT = "source-tenant";
    private static final String TARGET_TENANT = "target-tenant";
    private static final String PARTNER_LINK_ID = "pl-1";
    private static final Instant EXPIRES = Instant.parse("2026-04-01T00:00:00Z");
    private static final AuthPrincipal PRINCIPAL = new AuthPrincipal(
        "token1", "admin1", SOURCE_TENANT,
        VerificationLevel.PHONE_VERIFIED, Set.of("SCOPE_DELEGATION_GRANT"),
        Instant.parse("2026-03-02T00:00:00Z")
    );

    private static final PartnerLink ACTIVE_PARTNER_LINK = new PartnerLink(
        PARTNER_LINK_ID, SOURCE_TENANT, TARGET_TENANT, PartnerType.RETAIL,
        PartnerLinkStatus.ACTIVE, "admin1", Instant.parse("2026-01-01T00:00:00Z"),
        "approver1", Instant.parse("2026-01-02T00:00:00Z"), null, null, null
    );

    @BeforeEach
    void setUp() {
        service = new DelegationService(
            delegationRepository, tenantReadPort,
            passportAuthorityQueryPort, permissionProjectionPort,
            relationshipValidator, delegationGrantPolicy, authorizationSupport, clock
        );
    }

    @Test
    void batchGrant_allSuccess() {
        setupCommonMocks();
        stubPassport("p1");
        stubPassport("p2");
        stubNoDuplicate("p1");
        stubNoDuplicate("p2");
        when(delegationRepository.save(any(Delegation.class))).thenAnswer(inv -> inv.getArgument(0));

        BatchDelegationResult result = service.batchGrantPassportDelegation(
            PRINCIPAL, SOURCE_TENANT, PARTNER_LINK_ID,
            new BatchGrantPassportDelegationCommand(List.of("p1", "p2"), EXPIRES, "batch note")
        );

        assertEquals(2, result.totalRequested());
        assertEquals(2, result.totalGranted());
        assertEquals("GRANTED", result.results().get(0).status());
        assertEquals("GRANTED", result.results().get(1).status());
        assertEquals("p1", result.results().get(0).passportId());
        assertEquals("p2", result.results().get(1).passportId());
        verify(delegationRepository, times(2)).save(any(Delegation.class));
        verify(permissionProjectionPort, times(2)).onDelegationGranted(any(), eq("ACTIVE"));
    }

    @Test
    void batchGrant_partialSuccess_duplicateSkipped() {
        setupCommonMocks();
        stubPassport("p1");
        stubPassport("p2");
        stubNoDuplicate("p1");
        // p2 already has active delegation
        when(delegationRepository.existsActive(
            SOURCE_TENANT, TARGET_TENANT, "PASSPORT", "p2", "RETAIL_TRANSFER_CREATE"
        )).thenReturn(true);
        when(delegationRepository.save(any(Delegation.class))).thenAnswer(inv -> inv.getArgument(0));

        BatchDelegationResult result = service.batchGrantPassportDelegation(
            PRINCIPAL, SOURCE_TENANT, PARTNER_LINK_ID,
            new BatchGrantPassportDelegationCommand(List.of("p1", "p2"), EXPIRES, "note")
        );

        assertEquals(2, result.totalRequested());
        assertEquals(1, result.totalGranted());
        assertEquals("GRANTED", result.results().get(0).status());
        assertEquals("FAILED", result.results().get(1).status());
        assertEquals("DELEGATION_ALREADY_ACTIVE", result.results().get(1).error());
        verify(delegationRepository, times(1)).save(any(Delegation.class));
    }

    @Test
    void batchGrant_passportNotFound_fails() {
        setupCommonMocks();
        when(passportAuthorityQueryPort.findPassportAuthority("missing"))
            .thenReturn(Optional.empty());
        when(delegationRepository.existsActive(
            SOURCE_TENANT, TARGET_TENANT, "PASSPORT", "missing", "RETAIL_TRANSFER_CREATE"
        )).thenReturn(false);

        BatchDelegationResult result = service.batchGrantPassportDelegation(
            PRINCIPAL, SOURCE_TENANT, PARTNER_LINK_ID,
            new BatchGrantPassportDelegationCommand(List.of("missing"), EXPIRES, "note")
        );

        assertEquals(1, result.totalRequested());
        assertEquals(0, result.totalGranted());
        assertEquals("FAILED", result.results().get(0).status());
        assertEquals("INVALID_REQUEST", result.results().get(0).error());
        verify(delegationRepository, never()).save(any(Delegation.class));
    }

    @Test
    void batchGrant_voidedPassport_fails() {
        setupCommonMocks();
        when(passportAuthorityQueryPort.findPassportAuthority("voided"))
            .thenReturn(Optional.of(new PassportAuthorityView("voided", SOURCE_TENANT, "VOIDED", "NONE")));
        when(delegationRepository.existsActive(
            SOURCE_TENANT, TARGET_TENANT, "PASSPORT", "voided", "RETAIL_TRANSFER_CREATE"
        )).thenReturn(false);

        BatchDelegationResult result = service.batchGrantPassportDelegation(
            PRINCIPAL, SOURCE_TENANT, PARTNER_LINK_ID,
            new BatchGrantPassportDelegationCommand(List.of("voided"), EXPIRES, "note")
        );

        assertEquals(0, result.totalGranted());
        assertEquals("FAILED", result.results().get(0).status());
        assertEquals("INVALID_STATE", result.results().get(0).error());
    }

    @Test
    void batchGrant_emptyList_returnsEmpty() {
        setupCommonMocks();

        BatchDelegationResult result = service.batchGrantPassportDelegation(
            PRINCIPAL, SOURCE_TENANT, PARTNER_LINK_ID,
            new BatchGrantPassportDelegationCommand(List.of(), EXPIRES, "note")
        );

        assertEquals(0, result.totalRequested());
        assertEquals(0, result.totalGranted());
        assertEquals(List.of(), result.results());
        verify(delegationRepository, never()).save(any(Delegation.class));
    }

    private void setupCommonMocks() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(relationshipValidator.assertEligibleBySource(PARTNER_LINK_ID, SOURCE_TENANT))
            .thenReturn(ACTIVE_PARTNER_LINK);
        when(tenantReadPort.existsActiveTenant(SOURCE_TENANT)).thenReturn(true);
        when(tenantReadPort.existsActiveTenant(TARGET_TENANT)).thenReturn(true);
    }

    private void stubPassport(String passportId) {
        when(passportAuthorityQueryPort.findPassportAuthority(passportId))
            .thenReturn(Optional.of(new PassportAuthorityView(passportId, SOURCE_TENANT, "ACTIVE", "NONE")));
    }

    private void stubNoDuplicate(String passportId) {
        when(delegationRepository.existsActive(
            SOURCE_TENANT, TARGET_TENANT, "PASSPORT", passportId, "RETAIL_TRANSFER_CREATE"
        )).thenReturn(false);
    }
}

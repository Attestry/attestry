package io.attestry.workflow.application.servicerequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.identity.model.VerificationLevel;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.ServicePermissionPort;
import io.attestry.workflow.application.port.ServiceProductReadPort;
import io.attestry.workflow.application.port.ServiceProductReadPort.ServicePassportState;
import io.attestry.workflow.application.servicerequest.command.GrantServiceConsentCommand;
import io.attestry.workflow.application.servicerequest.result.GrantServiceConsentResult;
import io.attestry.workflow.application.servicerequest.result.RevokeServiceConsentResult;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ServiceSubmitUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.policy.ServiceConsentPolicy;
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
class ServiceConsentServiceTest {

    @Mock ServiceProductReadPort serviceProductReadPort;
    @Mock ServicePermissionPort servicePermissionPort;
    @Mock ServiceSubmitUseCase serviceSubmitUseCase;
    @Mock WorkflowAuthorizationSupport authorizationSupport;

    private final ServiceConsentPolicy consentPolicy = new ServiceConsentPolicy();
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    private ServiceConsentService service;

    private static final AuthPrincipal OWNER = new AuthPrincipal(
        "token1", "owner1", null, VerificationLevel.PHONE_VERIFIED, Set.of("SCOPE_OWNER_SERVICE_CREATE"), Instant.parse("2026-03-02T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new ServiceConsentService(
            serviceProductReadPort, servicePermissionPort, serviceSubmitUseCase, authorizationSupport, consentPolicy, clock
        );
    }

    @Test
    void grantConsent_success() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceProductReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new ServicePassportState("p1", "t1", "ACTIVE", "NONE")));
        when(serviceProductReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("owner1"));
        when(servicePermissionPort.grantServiceRepairConsent(anyString(), anyString(), anyString(), any(Instant.class)))
            .thenReturn("perm1");
        when(serviceSubmitUseCase.approve(any(), any())).thenReturn(
            new io.attestry.workflow.application.servicerequest.result.SubmitServiceRequestResult(
                "sr1", "p1", "provG1", "REPAIR", "PENDING", "perm1", Instant.parse("2026-03-01T10:00:00Z")
            )
        );

        GrantServiceConsentResult result = service.submit(
            OWNER, "p1", new GrantServiceConsentCommand("provG1", "eg1", "ONLINE", "화면 불량", null, "010-0000-0000 / 평일 오후")
        );

        assertEquals("perm1", result.permissionId());
        assertEquals("sr1", result.serviceRequestId());
        assertEquals("p1", result.passportId());
        assertEquals("provG1", result.providerTenantId());
        assertEquals("ACTIVE", result.consentStatus());
        assertEquals("PENDING", result.serviceRequestStatus());
        assertNotNull(result.grantedAt());
    }

    @Test
    void grantConsent_assetNotActive_throws() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceProductReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new ServicePassportState("p1", "t1", "VOIDED", "NONE")));
        when(serviceProductReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("owner1"));
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.submit(OWNER, "p1", new GrantServiceConsentCommand("provG1", "eg1", "ONLINE", "화면 불량", null, "010-0000-0000 / 평일 오후"))
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void grantConsent_riskFlagged_throws() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceProductReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new ServicePassportState("p1", "t1", "ACTIVE", "FLAGGED")));
        when(serviceProductReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("owner1"));
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.submit(OWNER, "p1", new GrantServiceConsentCommand("provG1", "eg1", "ONLINE", "화면 불량", null, "010-0000-0000 / 평일 오후"))
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void grantConsent_ownerMismatch_throws() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceProductReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new ServicePassportState("p1", "t1", "ACTIVE", "NONE")));
        when(serviceProductReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("differentOwner"));
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.submit(OWNER, "p1", new GrantServiceConsentCommand("provG1", "eg1", "ONLINE", "화면 불량", null, "010-0000-0000 / 평일 오후"))
        );
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }

    @Test
    void revokeConsent_success() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceProductReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("owner1"));

        RevokeServiceConsentResult result = service.revokeConsent(OWNER, "p1", "provG1");

        assertEquals("p1", result.passportId());
        assertEquals("provG1", result.providerTenantId());
        assertEquals("REVOKED", result.status());
        verify(servicePermissionPort).revokeConsentByPassportAndTenant("p1", "provG1");
    }

    @Test
    void revokeConsent_ownerMismatch_throws() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceProductReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("differentOwner"));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.revokeConsent(OWNER, "p1", "provG1")
        );
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }
}

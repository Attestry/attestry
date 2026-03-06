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
import io.attestry.workflow.application.servicerequest.result.CancelServiceRequestResult;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
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
class ServiceCancelServiceTest {

    @Mock ServiceRequestRepository serviceRequestRepository;
    @Mock ServicePermissionPort servicePermissionPort;
    @Mock WorkflowAuthorizationSupport authorizationSupport;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    private ServiceCancelService service;

    private static final Instant SUBMITTED_AT = Instant.parse("2026-03-01T09:00:00Z");
    private static final AuthPrincipal OWNER = new AuthPrincipal(
        "token1", "owner1", null, VerificationLevel.PHONE_VERIFIED, Set.of("SCOPE_OWNER_SERVICE_CREATE"), Instant.parse("2026-03-02T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new ServiceCancelService(
            serviceRequestRepository, servicePermissionPort, authorizationSupport, clock
        );
    }

    @Test
    void cancel_success() {
        ServiceRequest submitted = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "provT1", "desc", null, "perm1", "provider1", SUBMITTED_AT, SUBMITTED_AT
        );

        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceRequestRepository.findById("sr1")).thenReturn(Optional.of(submitted));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CancelServiceRequestResult result = service.cancel(OWNER, "sr1", "No longer needed");

        assertEquals("CANCELLED", result.status());
        assertEquals("p1", result.passportId());
        assertNotNull(result.cancelledAt());
        verify(servicePermissionPort).revokeByServiceRequestId("sr1");
    }

    @Test
    void cancel_ownerMismatch_throws() {
        ServiceRequest submitted = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "differentOwner",
            "provT1", "desc", null, null, "provider1", SUBMITTED_AT, SUBMITTED_AT
        );

        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceRequestRepository.findById("sr1")).thenReturn(Optional.of(submitted));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.cancel(OWNER, "sr1", "reason")
        );
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }

    @Test
    void cancel_notSubmitted_throws() {
        ServiceRequest submitted = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "provT1", "desc", null, null, "provider1", SUBMITTED_AT, SUBMITTED_AT
        );
        ServiceRequest completed = submitted.complete("provider1", "afterEg", Instant.parse("2026-03-01T10:00:00Z"));

        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceRequestRepository.findById("sr1")).thenReturn(Optional.of(completed));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.cancel(OWNER, "sr1", "reason")
        );
        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void cancel_notFound_throws() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceRequestRepository.findById("missing")).thenReturn(Optional.empty());

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.cancel(OWNER, "missing", "reason")
        );
        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_NOT_FOUND, ex.getErrorCode());
    }
}

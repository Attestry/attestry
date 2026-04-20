package io.attestry.workflow.application.servicerequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.auth.model.VerificationLevel;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.servicerequest.command.ServiceCancelService;
import io.attestry.workflow.application.port.servicerequest.ServicePermissionPort;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestAccessPolicy;
import io.attestry.workflow.application.servicerequest.result.CancelServiceRequestResult;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestResultFactory;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestLookupService;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceCancelServiceTest {

    @Mock ServiceRequestLookupService serviceRequestLookupService;
    @Mock io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository serviceRequestRepository;
    @Mock ServicePermissionPort servicePermissionPort;
    @Mock WorkflowAuthorizationSupport authorizationSupport;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    private ServiceCancelService service;

    private static final Instant SUBMITTED_AT = Instant.parse("2026-03-01T09:00:00Z");
    private static final WorkflowActorContext OWNER = new WorkflowActorContext(
        "token1", "owner1", null, VerificationLevel.PHONE_VERIFIED, Set.of("SCOPE_OWNER_SERVICE_CREATE"), Instant.parse("2026-03-02T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        ServiceRequestAccessPolicy accessPolicy = new ServiceRequestAccessPolicy(authorizationSupport);
        service = new ServiceCancelService(
            accessPolicy,
            serviceRequestLookupService,
            serviceRequestRepository,
            servicePermissionPort,
            new ServiceRequestResultFactory(),
            clock
        );
    }

    @Test
    void cancel_success() {
        ServiceRequest pending = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "provT1", "desc", "eg1", "ONLINE", "Screen defect", null, "contact", "perm1", "owner1", SUBMITTED_AT, SUBMITTED_AT
        );

        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceRequestLookupService.getPendingById("sr1")).thenReturn(pending);
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CancelServiceRequestResult result = service.cancel(OWNER, "sr1", "No longer needed");

        assertEquals("CANCELLED", result.status());
        assertEquals("p1", result.passportId());
        assertNotNull(result.cancelledAt());
        verify(servicePermissionPort).revokeByServiceRequestId("sr1");
    }

    @Test
    void cancel_ownerMismatch_throws() {
        ServiceRequest pending = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "differentOwner",
            "provT1", "desc", "eg1", "ONLINE", "Screen defect", null, "contact", null, "differentOwner", SUBMITTED_AT, SUBMITTED_AT
        );

        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceRequestLookupService.getPendingById("sr1")).thenReturn(pending);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.cancel(OWNER, "sr1", "reason")
        );
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }

    @Test
    void cancel_notAllowedState_throws() {
        ServiceRequest rejected = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "provT1", "desc", "eg1", "ONLINE", "Screen defect", null, "contact", null, "owner1", SUBMITTED_AT, SUBMITTED_AT
        ).reject("reason", Instant.parse("2026-03-01T09:30:00Z"));

        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceRequestLookupService.getPendingById("sr1"))
            .thenThrow(new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE, "Only PENDING service request can be processed"));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.cancel(OWNER, "sr1", "reason")
        );
        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void cancel_acceptedState_throws() {
        ServiceRequest accepted = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "provT1", "desc", "eg1", "ONLINE", "Screen defect", null, "contact", null, "owner1", SUBMITTED_AT, SUBMITTED_AT
        ).accept(
            "REPAIR",
            "Confirmed by staff",
            Instant.parse("2026-03-01T09:30:00Z")
        );

        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceRequestLookupService.getPendingById("sr1"))
            .thenThrow(new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE, "Only PENDING service request can be processed"));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.cancel(OWNER, "sr1", "reason")
        );
        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void cancel_notFound_throws() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceRequestLookupService.getPendingById("missing"))
            .thenThrow(new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_NOT_FOUND, "Service request not found"));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.cancel(OWNER, "missing", "reason")
        );
        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_NOT_FOUND, ex.getErrorCode());
    }
}

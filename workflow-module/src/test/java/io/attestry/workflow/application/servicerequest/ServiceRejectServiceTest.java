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
import io.attestry.workflow.application.servicerequest.command.RejectServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.command.ServiceRejectService;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestAccessPolicy;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestLookupService;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestResultFactory;
import io.attestry.workflow.application.servicerequest.result.RejectServiceRequestResult;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
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
class ServiceRejectServiceTest {

    @Mock ServiceRequestLookupService serviceRequestLookupService;
    @Mock ServiceRequestRepository serviceRequestRepository;
    @Mock WorkflowAuthorizationSupport authorizationSupport;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    private ServiceRejectService service;

    private static final Instant SUBMITTED_AT = Instant.parse("2026-03-01T09:00:00Z");
    private static final WorkflowActorContext PROVIDER = new WorkflowActorContext(
        "token1", "user1", "provT1", VerificationLevel.PHONE_VERIFIED,
        Set.of("SCOPE_SERVICE_COMPLETE"), Instant.parse("2026-03-02T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        ServiceRequestAccessPolicy accessPolicy = new ServiceRequestAccessPolicy(authorizationSupport);
        service = new ServiceRejectService(
            accessPolicy,
            serviceRequestLookupService,
            serviceRequestRepository,
            new ServiceRequestResultFactory(),
            clock
        );
    }

    @Test
    void reject_success() {
        ServiceRequest pending = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "provT1", "desc", "eg1", "ONLINE", "screen defect", null, "contact", "perm1", "owner1", SUBMITTED_AT, SUBMITTED_AT
        );
        RejectServiceRequestCommand command = new RejectServiceRequestCommand("Not eligible for repair");

        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(serviceRequestLookupService.getPendingById("sr1")).thenReturn(pending);
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RejectServiceRequestResult result = service.reject(PROVIDER, "provT1", "sr1", command);

        assertEquals("REJECTED", result.status());
        assertEquals("p1", result.passportId());
        assertNotNull(result.rejectedAt());
        verify(serviceRequestRepository).save(any(ServiceRequest.class));
    }

    @Test
    void reject_crossTenant_throws() {
        ServiceRequest pending = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "otherTenant", "desc", "eg1", "ONLINE", "screen defect", null, "contact", null, "owner1", SUBMITTED_AT, SUBMITTED_AT
        );

        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(serviceRequestLookupService.getPendingById("sr1")).thenReturn(pending);

        RejectServiceRequestCommand command = new RejectServiceRequestCommand("reason");

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.reject(PROVIDER, "provT1", "sr1", command)
        );
        assertEquals(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, ex.getErrorCode());
    }

    @Test
    void reject_notPending_throws() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(serviceRequestLookupService.getPendingById("sr1"))
            .thenThrow(new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE, "Only PENDING service request can be processed"));

        RejectServiceRequestCommand command = new RejectServiceRequestCommand("reason");

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.reject(PROVIDER, "provT1", "sr1", command)
        );
        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void reject_notFound_throws() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(serviceRequestLookupService.getPendingById("missing"))
            .thenThrow(new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_NOT_FOUND, "Service request not found"));

        RejectServiceRequestCommand command = new RejectServiceRequestCommand("reason");

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.reject(PROVIDER, "provT1", "missing", command)
        );
        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_NOT_FOUND, ex.getErrorCode());
    }
}

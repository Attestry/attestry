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
import io.attestry.workflow.application.port.ServiceLedgerOutboxPort;
import io.attestry.workflow.application.port.ServicePermissionPort;
import io.attestry.workflow.application.port.ServiceProductReadPort;
import io.attestry.workflow.application.port.ServiceProductReadPort.ServicePassportState;
import io.attestry.workflow.application.port.ShipmentEvidencePort;
import io.attestry.workflow.application.servicerequest.command.CompleteServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.CompleteServiceRequestResult;
import io.attestry.workflow.application.shipment.result.WorkflowLedgerEventEnvelope;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.policy.ServiceCompletePolicy;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
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
class ServiceCompleteServiceTest {

    @Mock ServiceRequestRepository serviceRequestRepository;
    @Mock ServiceProductReadPort serviceProductReadPort;
    @Mock ServicePermissionPort servicePermissionPort;
    @Mock ServiceLedgerOutboxPort serviceLedgerOutboxPort;
    @Mock ShipmentEvidencePort shipmentEvidencePort;
    @Mock WorkflowAuthorizationSupport authorizationSupport;

    private final ServiceCompletePolicy completePolicy = new ServiceCompletePolicy();
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    private ServiceCompleteService service;

    private static final Instant SUBMITTED_AT = Instant.parse("2026-03-01T09:00:00Z");
    private static final AuthPrincipal PROVIDER = new AuthPrincipal(
        "token1", "provider1", "provT1", "provG1", VerificationLevel.PHONE_VERIFIED, Set.of("SCOPE_SERVICE_COMPLETE"), Instant.parse("2026-03-02T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new ServiceCompleteService(
            serviceRequestRepository, serviceProductReadPort, servicePermissionPort,
            serviceLedgerOutboxPort, shipmentEvidencePort, authorizationSupport, completePolicy, clock
        );
    }

    @Test
    void complete_success() {
        ServiceRequest submitted = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "provT1", "provG1", "desc", "beforeEg", "perm1", "provider1", SUBMITTED_AT, SUBMITTED_AT
        );

        doNothing().when(authorizationSupport).assertTenantAndGroupContext(any(), anyString(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(serviceRequestRepository.findById("sr1")).thenReturn(Optional.of(submitted));
        when(serviceProductReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new ServicePassportState("p1", "t1", "g1", "ACTIVE", "NONE")));
        when(servicePermissionPort.hasActiveServiceRepairPermission("p1", "provG1")).thenReturn(true);
        when(shipmentEvidencePort.findReadyEvidenceHashes("afterEg")).thenReturn(List.of("hash1"));
        when(shipmentEvidencePort.findReadyEvidenceHashes("beforeEg")).thenReturn(List.of("beforeHash"));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(serviceLedgerOutboxPort.enqueue(any(WorkflowLedgerEventEnvelope.class))).thenReturn("outbox1");

        CompleteServiceRequestResult result = service.complete(
            PROVIDER, "provT1", "provG1", "sr1",
            new CompleteServiceRequestCommand("afterEg", "Repaired successfully")
        );

        assertEquals("COMPLETED", result.status());
        assertEquals("p1", result.passportId());
        assertNotNull(result.completedAt());
        assertEquals("outbox1", result.outboxEventId());
        verify(servicePermissionPort).revokeByServiceRequestId("sr1");
    }

    @Test
    void complete_notFound_throws() {
        doNothing().when(authorizationSupport).assertTenantAndGroupContext(any(), anyString(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(serviceRequestRepository.findById("missing")).thenReturn(Optional.empty());

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.complete(PROVIDER, "provT1", "provG1", "missing", new CompleteServiceRequestCommand(null, null))
        );
        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void complete_wrongTenant_throws() {
        ServiceRequest submitted = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "otherTenant", "otherGroup", "desc", null, null, "provider1", SUBMITTED_AT, SUBMITTED_AT
        );

        doNothing().when(authorizationSupport).assertTenantAndGroupContext(any(), anyString(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(serviceRequestRepository.findById("sr1")).thenReturn(Optional.of(submitted));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.complete(PROVIDER, "provT1", "provG1", "sr1", new CompleteServiceRequestCommand(null, null))
        );
        assertEquals(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, ex.getErrorCode());
    }

    @Test
    void complete_noPermission_throws() {
        ServiceRequest submitted = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "provT1", "provG1", "desc", null, null, "provider1", SUBMITTED_AT, SUBMITTED_AT
        );

        doNothing().when(authorizationSupport).assertTenantAndGroupContext(any(), anyString(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(serviceRequestRepository.findById("sr1")).thenReturn(Optional.of(submitted));
        when(serviceProductReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new ServicePassportState("p1", "t1", "g1", "ACTIVE", "NONE")));
        when(servicePermissionPort.hasActiveServiceRepairPermission("p1", "provG1")).thenReturn(false);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.complete(PROVIDER, "provT1", "provG1", "sr1", new CompleteServiceRequestCommand(null, null))
        );
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }
}

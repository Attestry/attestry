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
import io.attestry.workflow.application.port.ShipmentEvidencePort;
import io.attestry.workflow.application.servicerequest.command.SubmitServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.SubmitServiceRequestResult;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.policy.ServiceSubmitPolicy;
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
class ServiceSubmitServiceTest {

    @Mock ServiceRequestRepository serviceRequestRepository;
    @Mock ServiceProductReadPort serviceProductReadPort;
    @Mock ServicePermissionPort servicePermissionPort;
    @Mock ShipmentEvidencePort shipmentEvidencePort;
    @Mock WorkflowAuthorizationSupport authorizationSupport;

    private final ServiceSubmitPolicy submitPolicy = new ServiceSubmitPolicy();
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    private ServiceSubmitService service;

    private static final AuthPrincipal PROVIDER = new AuthPrincipal(
        "token1", "provider1", "provT1", "provG1", VerificationLevel.PHONE_VERIFIED, Set.of("SCOPE_SERVICE_COMPLETE"), Instant.parse("2026-03-02T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new ServiceSubmitService(
            serviceRequestRepository, serviceProductReadPort, servicePermissionPort,
            shipmentEvidencePort, authorizationSupport, submitPolicy, clock
        );
    }

    @Test
    void submit_success() {
        doNothing().when(authorizationSupport).assertTenantAndGroupContext(any(), anyString(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(serviceProductReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new ServicePassportState("p1", "t1", "g1", "ACTIVE", "NONE")));
        when(serviceProductReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("owner1"));
        when(servicePermissionPort.hasActiveServiceRepairPermission("p1", "provG1")).thenReturn(true);
        when(serviceRequestRepository.existsSubmittedByPassportId("p1")).thenReturn(false);
        when(servicePermissionPort.findActivePermissionId("p1", "provG1")).thenReturn(Optional.of("perm1"));
        when(shipmentEvidencePort.findReadyEvidenceHashes("eg1")).thenReturn(List.of("hash1"));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitServiceRequestResult result = service.submit(
            PROVIDER, "provT1", "provG1",
            new SubmitServiceRequestCommand("p1", "REPAIR", "Fix it", "eg1")
        );

        assertEquals("SUBMITTED", result.status());
        assertEquals("p1", result.passportId());
        assertEquals("REPAIR", result.serviceType());
        assertEquals("perm1", result.permissionId());
        assertNotNull(result.submittedAt());
        verify(servicePermissionPort).linkServiceRequest(anyString(), anyString());
    }

    @Test
    void submit_noConsentPermission_throws() {
        doNothing().when(authorizationSupport).assertTenantAndGroupContext(any(), anyString(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(serviceProductReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new ServicePassportState("p1", "t1", "g1", "ACTIVE", "NONE")));
        when(serviceProductReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("owner1"));
        when(servicePermissionPort.hasActiveServiceRepairPermission("p1", "provG1")).thenReturn(false);
        when(serviceRequestRepository.existsSubmittedByPassportId("p1")).thenReturn(false);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.submit(PROVIDER, "provT1", "provG1", new SubmitServiceRequestCommand("p1", "REPAIR", null, null))
        );
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }

    @Test
    void submit_duplicateSubmitted_throws() {
        doNothing().when(authorizationSupport).assertTenantAndGroupContext(any(), anyString(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(serviceProductReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new ServicePassportState("p1", "t1", "g1", "ACTIVE", "NONE")));
        when(serviceProductReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("owner1"));
        when(servicePermissionPort.hasActiveServiceRepairPermission("p1", "provG1")).thenReturn(true);
        when(serviceRequestRepository.existsSubmittedByPassportId("p1")).thenReturn(true);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.submit(PROVIDER, "provT1", "provG1", new SubmitServiceRequestCommand("p1", "REPAIR", null, null))
        );
        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_ALREADY_SUBMITTED, ex.getErrorCode());
    }

    @Test
    void submit_assetNotActive_throws() {
        doNothing().when(authorizationSupport).assertTenantAndGroupContext(any(), anyString(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(serviceProductReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new ServicePassportState("p1", "t1", "g1", "VOIDED", "NONE")));
        when(serviceProductReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("owner1"));
        when(servicePermissionPort.hasActiveServiceRepairPermission("p1", "provG1")).thenReturn(true);
        when(serviceRequestRepository.existsSubmittedByPassportId("p1")).thenReturn(false);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.submit(PROVIDER, "provT1", "provG1", new SubmitServiceRequestCommand("p1", "REPAIR", null, null))
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }
}

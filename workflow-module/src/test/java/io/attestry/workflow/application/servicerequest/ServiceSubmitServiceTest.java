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
import io.attestry.workflow.application.port.servicerequest.ServicePermissionPort;
import io.attestry.workflow.application.port.servicerequest.ServiceProductReadPort;
import io.attestry.workflow.application.port.servicerequest.ServiceProductReadPort.ServicePassportState;
import io.attestry.workflow.application.port.common.TenantReadPort;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
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
    @Mock TenantReadPort tenantReadPort;
    @Mock WorkflowEvidencePort shipmentEvidencePort;
    @Mock WorkflowAuthorizationSupport authorizationSupport;

    private final ServiceSubmitPolicy submitPolicy = new ServiceSubmitPolicy();
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);

    private ServiceSubmitService service;

    private static final AuthPrincipal OWNER = new AuthPrincipal(
        "token1", "owner1", null, VerificationLevel.PHONE_VERIFIED, Set.of("SCOPE_OWNER_SERVICE_CREATE"), Instant.parse("2026-03-02T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new ServiceSubmitService(
            serviceRequestRepository, serviceProductReadPort, servicePermissionPort,
            tenantReadPort, shipmentEvidencePort, authorizationSupport, submitPolicy, clock
        );
    }

    @Test
    void submit_success() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceProductReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new ServicePassportState("p1", "t1", "ACTIVE", "NONE")));
        when(serviceProductReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("owner1"));
        when(servicePermissionPort.hasActiveServiceRepairPermission("p1", "provT1")).thenReturn(true);
        when(serviceRequestRepository.existsOpenByPassportId("p1")).thenReturn(false);
        when(servicePermissionPort.findActivePermissionId("p1", "provT1")).thenReturn(Optional.of("perm1"));
        when(tenantReadPort.findTenantSummary("provT1"))
            .thenReturn(new TenantReadPort.TenantSummary("provT1", "Provider", "SEOUL", "서울시 강남구", "SERVICE"));
        when(shipmentEvidencePort.findReadyEvidenceHashes("eg1")).thenReturn(List.of("hash1"));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitServiceRequestResult result = service.approve(
            OWNER,
            new SubmitServiceRequestCommand("p1", "provT1", "eg1", "ONLINE", "화면 불량", null, "010-0000-0000 / 평일 오후")
        );

        assertEquals("PENDING", result.status());
        assertEquals("p1", result.passportId());
        assertEquals("provT1", result.providerTenantId());
        assertEquals(null, result.serviceType());
        assertEquals("perm1", result.permissionId());
        assertNotNull(result.submittedAt());
        verify(servicePermissionPort).linkServiceRequest(anyString(), anyString());
    }

    @Test
    void submit_noConsentPermission_throws() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceProductReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new ServicePassportState("p1", "t1", "ACTIVE", "NONE")));
        when(serviceProductReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("owner1"));
        when(servicePermissionPort.hasActiveServiceRepairPermission("p1", "provT1")).thenReturn(false);
        when(serviceRequestRepository.existsOpenByPassportId("p1")).thenReturn(false);
        when(tenantReadPort.findTenantSummary("provT1"))
            .thenReturn(new TenantReadPort.TenantSummary("provT1", "Provider", "SEOUL", "서울시 강남구", "SERVICE"));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.approve(OWNER, new SubmitServiceRequestCommand("p1", "provT1", "eg1", "ONLINE", "화면 불량", null, "010-0000-0000 / 평일 오후"))
        );
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }

    @Test
    void submit_duplicateOpenRequest_throws() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceProductReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new ServicePassportState("p1", "t1", "ACTIVE", "NONE")));
        when(serviceProductReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("owner1"));
        when(servicePermissionPort.hasActiveServiceRepairPermission("p1", "provT1")).thenReturn(true);
        when(serviceRequestRepository.existsOpenByPassportId("p1")).thenReturn(true);
        when(tenantReadPort.findTenantSummary("provT1"))
            .thenReturn(new TenantReadPort.TenantSummary("provT1", "Provider", "SEOUL", "서울시 강남구", "SERVICE"));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.approve(OWNER, new SubmitServiceRequestCommand("p1", "provT1", "eg1", "ONLINE", "화면 불량", null, "010-0000-0000 / 평일 오후"))
        );
        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_ALREADY_SUBMITTED, ex.getErrorCode());
    }

    @Test
    void submit_notCurrentOwner_throws() {
        doNothing().when(authorizationSupport).assertPermissionOnly(any(), anyString(), anyString());
        when(serviceProductReadPort.findPassportState("p1"))
            .thenReturn(Optional.of(new ServicePassportState("p1", "t1", "ACTIVE", "NONE")));
        when(serviceProductReadPort.findCurrentOwnerId("p1")).thenReturn(Optional.of("otherOwner"));
        when(tenantReadPort.findTenantSummary("provT1"))
            .thenReturn(new TenantReadPort.TenantSummary("provT1", "Provider", "SEOUL", "서울시 강남구", "SERVICE"));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            service.approve(OWNER, new SubmitServiceRequestCommand("p1", "provT1", "eg1", "ONLINE", "화면 불량", null, "010-0000-0000 / 평일 오후"))
        );
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }
}

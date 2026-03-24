package io.attestry.workflow.application.shipment;

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
import io.attestry.workflow.application.shipment.command.ShipmentReleaseService;
import io.attestry.workflow.application.port.common.UserReadPort;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.port.common.WorkflowLedgerOutboxPort;
import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort;
import io.attestry.workflow.application.shipment.command.ReleaseShipmentCommand;
import io.attestry.workflow.application.shipment.command.ReturnShipmentCommand;
import io.attestry.workflow.application.shipment.result.ReleaseShipmentResult;
import io.attestry.workflow.application.support.EvidenceUploadSupport;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.domain.shipment.model.ShipmentStatus;
import io.attestry.workflow.domain.shipment.policy.ShipmentReleasePolicy;
import io.attestry.workflow.domain.shipment.policy.ShipmentReturnPolicy;
import io.attestry.workflow.domain.shipment.repository.ShipmentRepository;
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
class ShipmentReleaseServiceTest {

    @Mock ShipmentRepository shipmentRepository;
    @Mock WorkflowEvidencePort evidencePort;
    @Mock ShipmentProductReadPort shipmentProductReadPort;
    @Mock WorkflowLedgerOutboxPort workflowLedgerOutboxPort;
    @Mock WorkflowAuthorizationSupport authorizationSupport;
    @Mock EvidenceUploadSupport evidenceUploadSupport;
    @Mock UserReadPort userReadPort;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-12T02:00:00Z"), ZoneOffset.UTC);

    private ShipmentReleaseService service;

    private static final WorkflowActorContext PRINCIPAL = new WorkflowActorContext(
        "token-1",
        "user-1",
        "tenant-1",
        VerificationLevel.PHONE_VERIFIED,
        Set.of("SCOPE_BRAND_RELEASE"),
        Instant.parse("2026-03-13T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new ShipmentReleaseService(
            shipmentRepository,
            evidencePort,
            shipmentProductReadPort,
            workflowLedgerOutboxPort,
            authorizationSupport,
            evidenceUploadSupport,
            new ShipmentReleasePolicy(),
            new ShipmentReturnPolicy(),
            userReadPort,
            clock
        );
    }

    @Test
    void release_savesShipmentAndEnqueuesOutbox() {
        doNothing().when(authorizationSupport).assertTenantContext(PRINCIPAL, "tenant-1");
        doNothing().when(authorizationSupport).assertLivePermission(
            PRINCIPAL, "tenant-1", PermissionCodes.BRAND_RELEASE, "shipment:release:passport-1"
        );
        when(shipmentProductReadPort.findPassportState("passport-1"))
            .thenReturn(Optional.of(new ShipmentProductReadPort.PassportState(
                "passport-1", "tenant-1", "ACTIVE", "NONE"
            )));
        when(shipmentRepository.existsActiveReleasedByPassportId("passport-1")).thenReturn(false);
        doNothing().when(evidenceUploadSupport).assertEvidenceGroupScope(evidencePort, "group-1", "tenant-1");
        when(evidencePort.findReadyEvidenceHashes("group-1")).thenReturn(List.of("hash-1"));
        when(shipmentRepository.nextShipmentRound("passport-1")).thenReturn(1);
        when(shipmentRepository.saveRelease(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowLedgerOutboxPort.enqueue(any())).thenReturn("outbox-1");

        ReleaseShipmentResult result = service.release(PRINCIPAL, "passport-1", new ReleaseShipmentCommand("group-1"));

        assertEquals("tenant-1", result.tenantId());
        assertEquals("passport-1", result.passportId());
        assertEquals("RELEASED", result.status());
        assertEquals("group-1", result.evidenceGroupId());
        assertEquals("outbox-1", result.outboxEventId());
        verify(shipmentRepository).saveRelease(any(Shipment.class));
        verify(workflowLedgerOutboxPort).enqueue(any());
    }

    @Test
    void release_requiresAtLeastOneReadyEvidence() {
        doNothing().when(authorizationSupport).assertTenantContext(PRINCIPAL, "tenant-1");
        doNothing().when(authorizationSupport).assertLivePermission(
            PRINCIPAL, "tenant-1", PermissionCodes.BRAND_RELEASE, "shipment:release:passport-1"
        );
        when(shipmentProductReadPort.findPassportState("passport-1"))
            .thenReturn(Optional.of(new ShipmentProductReadPort.PassportState(
                "passport-1", "tenant-1", "ACTIVE", "NONE"
            )));
        when(shipmentRepository.existsActiveReleasedByPassportId("passport-1")).thenReturn(false);
        doNothing().when(evidenceUploadSupport).assertEvidenceGroupScope(evidencePort, "group-1", "tenant-1");
        when(evidencePort.findReadyEvidenceHashes("group-1")).thenReturn(List.of());

        WorkflowDomainException ex = assertThrows(
            WorkflowDomainException.class,
            () -> service.release(PRINCIPAL, "passport-1", new ReleaseShipmentCommand("group-1"))
        );

        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
        verify(shipmentRepository, never()).saveRelease(any());
        verify(workflowLedgerOutboxPort, never()).enqueue(any());
    }

    @Test
    void returnShipment_rejectsCrossTenantShipment() {
        Shipment shipment = new Shipment(
            "shipment-1",
            "tenant-2",
            "passport-1",
            1,
            ShipmentStatus.RELEASED,
            Instant.parse("2026-03-11T00:00:00Z"),
            "user-9",
            "tenant-2",
            "group-1",
            null,
            null,
            null,
            Instant.parse("2026-03-11T00:00:00Z")
        );
        doNothing().when(authorizationSupport).assertTenantContext(PRINCIPAL, "tenant-1");
        doNothing().when(authorizationSupport).assertLivePermission(
            PRINCIPAL, "tenant-1", PermissionCodes.BRAND_RELEASE, "shipment:return:shipment-1"
        );
        when(shipmentRepository.findByShipmentId("shipment-1")).thenReturn(Optional.of(shipment));

        WorkflowDomainException ex = assertThrows(
            WorkflowDomainException.class,
            () -> service.returnShipment(PRINCIPAL, "shipment-1", new ReturnShipmentCommand(null, null))
        );

        assertEquals(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, ex.getErrorCode());
        verify(shipmentRepository, never()).saveReturn(any());
    }
}

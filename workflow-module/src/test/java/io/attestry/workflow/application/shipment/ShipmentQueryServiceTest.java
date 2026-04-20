package io.attestry.workflow.application.shipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.domain.auth.model.VerificationLevel;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.shipment.query.ShipmentQueryService;
import io.attestry.workflow.application.port.common.UserReadPort;
import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort;
import io.attestry.workflow.application.shipment.internal.ShipmentEvidenceViewAssembler;
import io.attestry.workflow.application.shipment.internal.ShipmentQueryViewAssembler;
import io.attestry.workflow.application.shipment.internal.ShipmentQueryAccessPolicy;
import io.attestry.workflow.application.shipment.query.ShipmentDetailView;
import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.domain.shipment.model.ShipmentStatus;
import io.attestry.workflow.domain.shipment.repository.ShipmentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipmentQueryServiceTest {

    @Mock ShipmentRepository shipmentRepository;
    @Mock ShipmentProductReadPort shipmentProductReadPort;
    @Mock UserReadPort userReadPort;
    @Mock ShipmentQueryAccessPolicy accessPolicy;
    @Mock ShipmentEvidenceViewAssembler evidenceViewAssembler;

    private final ShipmentQueryViewAssembler viewAssembler = new ShipmentQueryViewAssembler();

    private ShipmentQueryService service;

    private static final WorkflowActorContext PRINCIPAL = new WorkflowActorContext(
        "token-1",
        "user-1",
        "tenant-1",
        VerificationLevel.PHONE_VERIFIED,
        Set.of("SCOPE_TENANT_READ_ONLY"),
        Instant.parse("2026-03-13T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new ShipmentQueryService(
            shipmentRepository,
            shipmentProductReadPort,
            userReadPort,
            accessPolicy,
            evidenceViewAssembler,
            viewAssembler
        );
    }

    @Test
    void listByPassport_filtersOtherTenantShipments() {
        Shipment ownShipment = shipment("shipment-1", "tenant-1", "passport-1");
        Shipment otherShipment = shipment("shipment-2", "tenant-2", "passport-1");
        when(accessPolicy.assertPassportListAccess(PRINCIPAL, "passport-1")).thenReturn("tenant-1");
        when(shipmentRepository.findByPassportId("passport-1")).thenReturn(List.of(ownShipment, otherShipment));
        when(shipmentProductReadPort.findPassportAssetInfoByIds(List.of("passport-1")))
            .thenReturn(Map.of(
                "passport-1",
                new ShipmentProductReadPort.PassportAssetInfo(
                    "passport-1", "asset-1", "SN-1", "model-1", "Model 1", "batch", "factory"
                )
            ));

        var result = service.listByPassport(PRINCIPAL, "passport-1");

        assertEquals(1, result.size());
        assertEquals("shipment-1", result.get(0).shipmentId());
    }

    @Test
    void getShipmentDetail_enrichesEvidenceAssetAndEmails() {
        Shipment shipment = new Shipment(
            "shipment-1",
            "tenant-1",
            "passport-1",
            1,
            ShipmentStatus.RELEASED,
            Instant.parse("2026-03-11T00:00:00Z"),
            "release-user",
            "tenant-1",
            "release-group",
            Instant.parse("2026-03-12T00:00:00Z"),
            "return-user",
            "return-group",
            Instant.parse("2026-03-11T00:00:00Z")
        );
        when(shipmentRepository.findByShipmentId("shipment-1")).thenReturn(Optional.of(shipment));
        when(evidenceViewAssembler.toDetailEvidenceFiles("release-group"))
            .thenReturn(List.of(new ShipmentDetailView.EvidenceFileView("e1", "r1.jpg", "image/jpeg", 10L, "u1")));
        when(evidenceViewAssembler.toDetailEvidenceFiles("return-group"))
            .thenReturn(List.of(new ShipmentDetailView.EvidenceFileView("e2", "r2.jpg", "image/jpeg", 20L, "u2")));
        when(shipmentProductReadPort.findPassportAssetInfoByIds(List.of("passport-1")))
            .thenReturn(Map.of(
                "passport-1",
                new ShipmentProductReadPort.PassportAssetInfo(
                    "passport-1", "asset-1", "SN-1", "model-1", "Model 1", "batch", "factory"
                )
            ));
        when(userReadPort.findEmailMapByUserIds(List.of("release-user", "return-user")))
            .thenReturn(Map.of(
                "release-user", "release@test.com",
                "return-user", "return@test.com"
            ));

        ShipmentDetailView result = service.getShipmentDetail(PRINCIPAL, "shipment-1");

        assertEquals("Model 1", result.modelName());
        assertEquals("SN-1", result.serialNumber());
        assertEquals("release@test.com", result.releasedByUserEmail());
        assertEquals("return@test.com", result.returnedByUserEmail());
        assertEquals(1, result.releaseEvidenceFiles().size());
        assertEquals(1, result.returnEvidenceFiles().size());
        verify(accessPolicy).assertShipmentDetailAccess(PRINCIPAL, shipment);
    }

    private Shipment shipment(String shipmentId, String tenantId, String passportId) {
        return new Shipment(
            shipmentId,
            tenantId,
            passportId,
            1,
            ShipmentStatus.RELEASED,
            Instant.parse("2026-03-11T00:00:00Z"),
            "user-1",
            tenantId,
            "group-1",
            null,
            null,
            null,
            Instant.parse("2026-03-11T00:00:00Z")
        );
    }
}

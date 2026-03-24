package io.attestry.product.application.port.projection;

import java.time.Instant;
import java.util.List;

public interface ProductShipmentProjectionWritePort {

    void refreshShipmentProjection(ShipmentPayload payload, String sourceEventId, Long sourceEventVersion, Instant updatedAt);

    record ShipmentPayload(
        String passportId,
        String shipmentId,
        String status,
        int shipmentRound,
        Instant releasedAt,
        String releasedByUserDisplay,
        Instant returnedAt,
        String returnedByUserDisplay,
        List<EvidencePayload> evidences
    ) {
    }

    record EvidencePayload(
        String evidenceId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String objectKey
    ) {
    }
}

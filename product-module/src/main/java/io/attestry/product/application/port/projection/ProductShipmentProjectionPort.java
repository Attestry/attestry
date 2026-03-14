package io.attestry.product.application.port.projection;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ProductShipmentProjectionPort {

    Optional<ShipmentProjectionView> findLatestShipment(String passportId);

    record ShipmentProjectionView(
        String passportId,
        String shipmentId,
        String status,
        int shipmentRound,
        Instant releasedAt,
        String releasedByUserDisplay,
        Instant returnedAt,
        String returnedByUserDisplay,
        List<ShipmentEvidenceProjectionView> evidenceFiles,
        Instant updatedAt
    ) {
    }

    record ShipmentEvidenceProjectionView(
        String evidenceId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String objectKey
    ) {
    }
}

package io.attestry.product.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PassportShipmentQueryPort {

    Optional<ShipmentView> findLatestShipmentByPassportId(String passportId);

    record ShipmentView(
        String shipmentId,
        String status,
        int shipmentRound,
        Instant releasedAt,
        String releasedByUserId,
        Instant returnedAt,
        String returnedByUserId,
        List<EvidenceFileView> evidenceFiles
    ) {
    }

    record EvidenceFileView(
        String evidenceId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String downloadUrl
    ) {
    }
}

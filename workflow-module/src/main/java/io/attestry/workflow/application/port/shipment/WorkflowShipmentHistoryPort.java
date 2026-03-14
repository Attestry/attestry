package io.attestry.workflow.application.port.shipment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkflowShipmentHistoryPort {

    Optional<ShipmentRecord> findLatestShipmentByPassportId(String passportId);

    record ShipmentRecord(
        String shipmentId,
        String status,
        int shipmentRound,
        Instant releasedAt,
        String releasedByUserEmail,
        Instant returnedAt,
        String returnedByUserEmail,
        List<EvidenceFileRecord> evidenceFiles
    ) {
    }

    record EvidenceFileRecord(
        String evidenceId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String downloadUrl
    ) {
    }
}

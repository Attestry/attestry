package io.attestry.workflow.application.shipment.view;

import java.time.Instant;
import java.util.List;

public record ShipmentDetailView(
    String shipmentId,
    String tenantId,
    String passportId,
    String modelName,
    String serialNumber,
    int shipmentRound,
    String status,
    Instant releasedAt,
    String releasedByUserEmail,
    Instant returnedAt,
    String returnedByUserEmail,
    List<EvidenceFileView> releaseEvidenceFiles,
    List<EvidenceFileView> returnEvidenceFiles,
    Instant createdAt
) {
    public record EvidenceFileView(
        String evidenceId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String downloadUrl
    ) {
    }
}

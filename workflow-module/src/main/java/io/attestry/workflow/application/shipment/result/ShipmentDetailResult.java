package io.attestry.workflow.application.shipment.result;

import java.time.Instant;
import java.util.List;

public record ShipmentDetailResult(
    String shipmentId,
    String tenantId,
    String passportId,
    int shipmentRound,
    String status,
    Instant releasedAt,
    String releasedByUserId,
    Instant returnedAt,
    String returnedByUserId,
    List<EvidenceFileResult> releaseEvidenceFiles,
    List<EvidenceFileResult> returnEvidenceFiles,
    Instant createdAt
) {
    public record EvidenceFileResult(
        String evidenceId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String downloadUrl
    ) {
    }
}

package io.attestry.workflow.application.shipment.result;

import java.time.Instant;
import java.util.List;

public record ShipmentDetailResult(
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
        List<EvidenceFileResult> releaseEvidenceFiles,
        List<EvidenceFileResult> returnEvidenceFiles,
        Instant createdAt) {
    public record EvidenceFileResult(
            String evidenceId,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String downloadUrl) {
    }
}

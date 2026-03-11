package io.attestry.workflow.interfaces.shipment.dto.response;

import io.attestry.workflow.application.shipment.result.ShipmentDetailResult;
import java.time.Instant;
import java.util.List;

public record ShipmentDetailResponse(
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
        List<EvidenceFileResponse> releaseEvidenceFiles,
        List<EvidenceFileResponse> returnEvidenceFiles,
        Instant createdAt) {
    public static ShipmentDetailResponse from(ShipmentDetailResult result) {
        return new ShipmentDetailResponse(
                result.shipmentId(),
                result.tenantId(),
                result.passportId(),
                result.modelName(),
                result.serialNumber(),
                result.shipmentRound(),
                result.status(),
                result.releasedAt(),
                result.releasedByUserEmail(),
                result.returnedAt(),
                result.returnedByUserEmail(),
                result.releaseEvidenceFiles().stream()
                        .map(EvidenceFileResponse::from)
                        .toList(),
                result.returnEvidenceFiles().stream()
                        .map(EvidenceFileResponse::from)
                        .toList(),
                result.createdAt());
    }
}

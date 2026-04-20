package io.attestry.workflow.interfaces.shipment.dto.response;

import io.attestry.workflow.application.shipment.command.PresignedEvidenceUploadResult;
import java.time.Instant;

public record PresignedShipmentEvidenceUploadResponse(
        String evidenceGroupId,
        String evidenceId,
        String objectKey,
        String uploadUrl,
        Instant expiresAt) {
    public static PresignedShipmentEvidenceUploadResponse from(PresignedEvidenceUploadResult result) {
        return new PresignedShipmentEvidenceUploadResponse(
                result.evidenceGroupId(),
                result.evidenceId(),
                result.objectKey(),
                result.uploadUrl(),
                result.expiresAt());
    }
}

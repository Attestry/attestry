package io.attestry.workflow.interfaces.manual.dto.response;

import io.attestry.workflow.application.shipment.command.PresignedEvidenceUploadResult;
import java.time.Instant;

public record PresignPassportManualEvidenceResponse(
    String evidenceGroupId,
    String evidenceId,
    String objectKey,
    String uploadUrl,
    Instant expiresAt
) {

    public static PresignPassportManualEvidenceResponse from(PresignedEvidenceUploadResult result) {
        return new PresignPassportManualEvidenceResponse(
            result.evidenceGroupId(),
            result.evidenceId(),
            result.objectKey(),
            result.uploadUrl(),
            result.expiresAt()
        );
    }
}

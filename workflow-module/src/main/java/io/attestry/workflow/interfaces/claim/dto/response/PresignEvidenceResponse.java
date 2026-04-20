package io.attestry.workflow.interfaces.claim.dto.response;

import io.attestry.workflow.application.shipment.command.PresignedEvidenceUploadResult;
import java.time.Instant;

public record PresignEvidenceResponse(
    String evidenceGroupId, String evidenceId,
    String objectKey, String uploadUrl, Instant expiresAt
) {
    public static PresignEvidenceResponse from(PresignedEvidenceUploadResult result) {
        return new PresignEvidenceResponse(
            result.evidenceGroupId(), result.evidenceId(),
            result.objectKey(), result.uploadUrl(), result.expiresAt()
        );
    }
}

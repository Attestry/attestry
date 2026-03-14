package io.attestry.userauth.interfaces.onboarding.dto.response;

import io.attestry.userauth.application.dto.result.PresignedEvidenceUploadResult;
import java.time.Instant;

public record PresignedEvidenceUploadResponse(
    String evidenceBundleId,
    String evidenceFileId,
    String objectKey,
    String uploadUrl,
    Instant expiresAt
) {
    public static PresignedEvidenceUploadResponse from(PresignedEvidenceUploadResult result) {
        return new PresignedEvidenceUploadResponse(
            result.evidenceBundleId(),
            result.evidenceFileId(),
            result.objectKey(),
            result.uploadUrl(),
            result.expiresAt()
        );
    }
}

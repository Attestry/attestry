package io.attestry.userauth.application.onboarding.result;

import java.time.Instant;

public record PresignedEvidenceUploadResult(
    String evidenceBundleId,
    String evidenceFileId,
    String objectKey,
    String uploadUrl,
    Instant expiresAt
) {
}

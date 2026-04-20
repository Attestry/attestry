package io.attestry.userauth.interfaces.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CompleteEvidenceUploadRequest(
    @NotBlank(message = "Evidence bundle ID is required")
    String evidenceBundleId,

    @NotBlank(message = "Evidence file ID is required")
    String evidenceFileId,

    @Positive(message = "File size must be greater than 0")
    long sizeBytes
) {
}

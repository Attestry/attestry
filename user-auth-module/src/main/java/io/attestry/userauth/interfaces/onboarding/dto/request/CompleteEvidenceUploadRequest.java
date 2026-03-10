package io.attestry.userauth.interfaces.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CompleteEvidenceUploadRequest(
    @NotBlank(message = "evidenceBundleId is required")
    String evidenceBundleId,

    @NotBlank(message = "evidenceFileId is required")
    String evidenceFileId,

    @Positive(message = "sizeBytes must be positive")
    long sizeBytes
) {
}

package io.attestry.workflow.interfaces.claim.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CompleteEvidenceRequest(
    @NotBlank(message = "Evidence group ID is required")
    String evidenceGroupId,
    @NotBlank(message = "Evidence ID is required")
    String evidenceId,
    @Positive(message = "Size in bytes must be positive")
    long sizeBytes,
    String fileHash
) {
}

package io.attestry.userauth.interfaces.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PresignEvidenceUploadRequest(
    String evidenceBundleId,
    @NotBlank(message = "File name is required")
    String fileName,
    @NotBlank(message = "Content type is required")
    String contentType
) {
}

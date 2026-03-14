package io.attestry.userauth.interfaces.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PresignEvidenceUploadRequest(
    String evidenceBundleId,
    String fileName,
    String contentType
) {
}

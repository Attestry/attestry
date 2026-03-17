package io.attestry.userauth.interfaces.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CompleteEvidenceUploadRequest(
    @NotBlank(message = "증빙 묶음 ID는 필수입니다")
    String evidenceBundleId,

    @NotBlank(message = "증빙 파일 ID는 필수입니다")
    String evidenceFileId,

    @Positive(message = "파일 크기는 0보다 커야 합니다")
    long sizeBytes
) {
}

package io.attestry.userauth.interfaces.onboarding.dto.request;

public record PresignEvidenceUploadRequest(
    String evidenceBundleId,
    String fileName,
    String contentType
) {
}

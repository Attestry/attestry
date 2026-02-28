package io.attestry.userauth.interfaces.onboarding.dto.request;

public record CompleteEvidenceUploadRequest(
    String evidenceBundleId,
    String evidenceFileId,
    long sizeBytes
) {
}

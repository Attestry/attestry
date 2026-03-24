package io.attestry.userauth.application.onboarding.command;

public record CompleteEvidenceUploadCommand(
    String evidenceBundleId,
    String evidenceFileId,
    long sizeBytes
) {
}

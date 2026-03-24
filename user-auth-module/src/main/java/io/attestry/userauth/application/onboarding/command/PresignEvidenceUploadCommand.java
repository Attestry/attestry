package io.attestry.userauth.application.onboarding.command;

public record PresignEvidenceUploadCommand(
    String evidenceBundleId,
    String fileName,
    String contentType
) {
}

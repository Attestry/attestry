package io.attestry.userauth.application.dto.command;

public record CompleteEvidenceUploadCommand(
    String evidenceBundleId,
    String evidenceFileId,
    long sizeBytes
) {
}

package io.attestry.userauth.application.dto.command;

public record PresignEvidenceUploadCommand(
    String evidenceBundleId,
    String fileName,
    String contentType
) {
}

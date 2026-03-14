package io.attestry.workflow.application.claim.command;

public record CompleteClaimEvidenceCommand(
    String evidenceGroupId,
    String evidenceId,
    long sizeBytes,
    String fileHash
) {
}

package io.attestry.workflow.application.claim.command;

public record CompleteClaimEvidenceCommand(
    String tenantId,
    String groupId,
    String evidenceGroupId,
    String evidenceId,
    long sizeBytes,
    String fileHash
) {
}

package io.attestry.workflow.application.claim.command;

public record PresignClaimEvidenceCommand(
    String tenantId,
    String evidenceGroupId,
    String fileName,
    String contentType
) {
}

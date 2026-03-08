package io.attestry.workflow.application.claim.command;

public record PresignClaimEvidenceCommand(
    String evidenceGroupId,
    String fileName,
    String contentType
) {
}

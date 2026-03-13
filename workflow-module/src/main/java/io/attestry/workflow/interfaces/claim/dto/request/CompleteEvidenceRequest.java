package io.attestry.workflow.interfaces.claim.dto.request;

public record CompleteEvidenceRequest(
    String evidenceGroupId, String evidenceId,
    long sizeBytes, String fileHash
) {
}

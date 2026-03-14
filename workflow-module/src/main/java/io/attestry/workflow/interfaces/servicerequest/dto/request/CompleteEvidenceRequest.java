package io.attestry.workflow.interfaces.servicerequest.dto.request;

public record CompleteEvidenceRequest(
    String evidenceGroupId,
    String evidenceId,
    long sizeBytes,
    String fileHash
) {
}

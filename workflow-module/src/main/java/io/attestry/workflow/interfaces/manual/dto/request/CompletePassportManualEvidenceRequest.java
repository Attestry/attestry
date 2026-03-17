package io.attestry.workflow.interfaces.manual.dto.request;

public record CompletePassportManualEvidenceRequest(
    String evidenceGroupId,
    String evidenceId,
    long sizeBytes,
    String fileHash
) {
}

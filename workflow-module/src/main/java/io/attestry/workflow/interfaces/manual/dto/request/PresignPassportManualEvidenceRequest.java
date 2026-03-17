package io.attestry.workflow.interfaces.manual.dto.request;

public record PresignPassportManualEvidenceRequest(
    String evidenceGroupId,
    String fileName,
    String contentType
) {
}

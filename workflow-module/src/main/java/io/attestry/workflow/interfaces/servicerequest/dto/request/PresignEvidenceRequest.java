package io.attestry.workflow.interfaces.servicerequest.dto.request;

public record PresignEvidenceRequest(
    String evidenceGroupId,
    String fileName,
    String contentType
) {
}

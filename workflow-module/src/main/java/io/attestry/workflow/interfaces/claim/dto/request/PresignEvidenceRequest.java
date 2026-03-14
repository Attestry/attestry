package io.attestry.workflow.interfaces.claim.dto.request;

public record PresignEvidenceRequest(
    String evidenceGroupId, String fileName, String contentType
) {
}

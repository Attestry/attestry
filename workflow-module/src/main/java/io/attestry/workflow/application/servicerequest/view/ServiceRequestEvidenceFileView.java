package io.attestry.workflow.application.servicerequest.view;

public record ServiceRequestEvidenceFileView(
    String evidenceId,
    String originalFileName,
    String contentType,
    long sizeBytes,
    String downloadUrl
) {
}

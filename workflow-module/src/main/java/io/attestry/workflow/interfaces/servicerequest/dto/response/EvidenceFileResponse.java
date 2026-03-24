package io.attestry.workflow.interfaces.servicerequest.dto.response;

import io.attestry.workflow.application.servicerequest.view.ServiceRequestEvidenceFileView;

public record EvidenceFileResponse(
    String evidenceId,
    String originalFileName,
    String contentType,
    long sizeBytes,
    String downloadUrl
) {
    public static EvidenceFileResponse from(ServiceRequestEvidenceFileView result) {
        return new EvidenceFileResponse(
            result.evidenceId(),
            result.originalFileName(),
            result.contentType(),
            result.sizeBytes(),
            result.downloadUrl()
        );
    }
}

package io.attestry.workflow.interfaces.servicerequest.dto.response;

import io.attestry.workflow.application.usecase.ServiceRequestQueryUseCase;

public record EvidenceFileResponse(
    String evidenceId,
    String originalFileName,
    String contentType,
    long sizeBytes,
    String downloadUrl
) {
    public static EvidenceFileResponse from(ServiceRequestQueryUseCase.EvidenceFileResult result) {
        return new EvidenceFileResponse(
            result.evidenceId(),
            result.originalFileName(),
            result.contentType(),
            result.sizeBytes(),
            result.downloadUrl()
        );
    }
}

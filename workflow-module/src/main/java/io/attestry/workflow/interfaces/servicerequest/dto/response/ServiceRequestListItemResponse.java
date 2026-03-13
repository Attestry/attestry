package io.attestry.workflow.interfaces.servicerequest.dto.response;

import io.attestry.workflow.application.usecase.ServiceRequestQueryUseCase;
import java.time.Instant;
import java.util.List;

public record ServiceRequestListItemResponse(
    String serviceRequestId,
    String passportId,
    String serialNumber,
    String modelName,
    String providerTenantId,
    String providerTenantName,
    String serviceType,
    String description,
    String serviceRequestMethod,
    String symptomDescription,
    Instant requestedReservationAt,
    String contactMemo,
    String beforeEvidenceGroupId,
    List<EvidenceFileResponse> beforeEvidenceFiles,
    String afterEvidenceGroupId,
    List<EvidenceFileResponse> afterEvidenceFiles,
    String serviceResultDetail,
    String completionMemo,
    String rejectReason,
    String cancelReason,
    String status,
    Instant submittedAt,
    Instant completedAt
) {
    public static ServiceRequestListItemResponse from(ServiceRequestQueryUseCase.ServiceRequestListItemResult result) {
        return new ServiceRequestListItemResponse(
            result.serviceRequestId(),
            result.passportId(),
            result.serialNumber(),
            result.modelName(),
            result.providerTenantId(),
            result.providerTenantName(),
            result.serviceType(),
            result.description(),
            result.serviceRequestMethod(),
            result.symptomDescription(),
            result.requestedReservationAt(),
            result.contactMemo(),
            result.beforeEvidenceGroupId(),
            result.beforeEvidenceFiles().stream().map(EvidenceFileResponse::from).toList(),
            result.afterEvidenceGroupId(),
            result.afterEvidenceFiles().stream().map(EvidenceFileResponse::from).toList(),
            result.serviceResultDetail(),
            result.completionMemo(),
            result.rejectReason(),
            result.cancelReason(),
            result.status(),
            result.submittedAt(),
            result.completedAt()
        );
    }
}

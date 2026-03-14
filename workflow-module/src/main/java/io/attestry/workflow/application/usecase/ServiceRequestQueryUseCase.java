package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import java.time.Instant;
import java.util.List;

public interface ServiceRequestQueryUseCase {

    PagedServiceRequestResult listMyRequests(AuthPrincipal principal, String status, int page, int size);

    PagedServiceRequestResult listProviderRequests(AuthPrincipal principal, String tenantId, String status, int page, int size);

    record ServiceRequestListItemResult(
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
        List<EvidenceFileResult> beforeEvidenceFiles,
        String afterEvidenceGroupId,
        List<EvidenceFileResult> afterEvidenceFiles,
        String serviceResultDetail,
        String completionMemo,
        String rejectReason,
        String cancelReason,
        String status,
        Instant submittedAt,
        Instant completedAt
    ) {
    }

    record EvidenceFileResult(
        String evidenceId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String downloadUrl
    ) {
    }

    record PagedServiceRequestResult(
        List<ServiceRequestListItemResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }
}

package io.attestry.workflow.application.servicerequest.view;

import java.time.Instant;
import java.util.List;

public record ServiceRequestListItemView(
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
    List<ServiceRequestEvidenceFileView> beforeEvidenceFiles,
    String afterEvidenceGroupId,
    List<ServiceRequestEvidenceFileView> afterEvidenceFiles,
    String serviceResultDetail,
    String completionMemo,
    String rejectReason,
    String cancelReason,
    String status,
    Instant submittedAt,
    Instant completedAt
) {
}

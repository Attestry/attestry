package io.attestry.workflow.domain.servicerequest.model;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Instant;

public record ServiceRequest(
    String serviceRequestId,
    String passportId,
    String serviceType,
    String ownerUserId,
    String providerTenantId,
    String providerGroupId,
    ServiceRequestStatus status,
    String description,
    String beforeEvidenceGroupId,
    String afterEvidenceGroupId,
    String permissionId,
    String submittedByUserId,
    Instant submittedAt,
    Instant completedAt,
    String completedByUserId,
    Instant cancelledAt,
    String cancelReason,
    Instant createdAt
) {

    public static ServiceRequest submit(
        String serviceRequestId,
        String passportId,
        String serviceType,
        String ownerUserId,
        String providerTenantId,
        String providerGroupId,
        String description,
        String beforeEvidenceGroupId,
        String permissionId,
        String submittedByUserId,
        Instant submittedAt,
        Instant createdAt
    ) {
        requireText(serviceRequestId, "serviceRequestId");
        requireText(passportId, "passportId");
        requireText(serviceType, "serviceType");
        requireText(ownerUserId, "ownerUserId");
        requireText(providerTenantId, "providerTenantId");
        requireText(providerGroupId, "providerGroupId");
        requireText(submittedByUserId, "submittedByUserId");
        requireNonNull(submittedAt, "submittedAt");
        requireNonNull(createdAt, "createdAt");

        return new ServiceRequest(
            serviceRequestId,
            passportId,
            serviceType,
            ownerUserId,
            providerTenantId,
            providerGroupId,
            ServiceRequestStatus.SUBMITTED,
            description,
            beforeEvidenceGroupId,
            null,
            permissionId,
            submittedByUserId,
            submittedAt,
            null,
            null,
            null,
            null,
            createdAt
        );
    }

    public ServiceRequest complete(String completedByUserId, String afterEvidenceGroupId, Instant now) {
        requireText(completedByUserId, "completedByUserId");
        requireNonNull(now, "now");
        if (status != ServiceRequestStatus.SUBMITTED) {
            throw new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE,
                "Only SUBMITTED service request can be completed");
        }
        return new ServiceRequest(
            serviceRequestId,
            passportId,
            serviceType,
            ownerUserId,
            providerTenantId,
            providerGroupId,
            ServiceRequestStatus.COMPLETED,
            description,
            beforeEvidenceGroupId,
            afterEvidenceGroupId,
            permissionId,
            submittedByUserId,
            submittedAt,
            now,
            completedByUserId,
            null,
            null,
            createdAt
        );
    }

    public ServiceRequest cancel(String cancelReason, Instant now) {
        requireNonNull(now, "now");
        if (status != ServiceRequestStatus.SUBMITTED) {
            throw new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE,
                "Only SUBMITTED service request can be cancelled");
        }
        return new ServiceRequest(
            serviceRequestId,
            passportId,
            serviceType,
            ownerUserId,
            providerTenantId,
            providerGroupId,
            ServiceRequestStatus.CANCELLED,
            description,
            beforeEvidenceGroupId,
            afterEvidenceGroupId,
            permissionId,
            submittedByUserId,
            submittedAt,
            null,
            null,
            now,
            cancelReason,
            createdAt
        );
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, field + " is required");
        }
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, field + " is required");
        }
    }
}

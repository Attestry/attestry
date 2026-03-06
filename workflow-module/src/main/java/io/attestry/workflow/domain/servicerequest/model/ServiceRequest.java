package io.attestry.workflow.domain.servicerequest.model;

import static io.attestry.workflow.domain.WorkflowValidation.requireNonNull;
import static io.attestry.workflow.domain.WorkflowValidation.requireText;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Instant;

public record ServiceRequest(
    String serviceRequestId,
    String passportId,
    String serviceType,
    String ownerUserId,
    String providerTenantId,
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
        requireText(submittedByUserId, "submittedByUserId");
        requireNonNull(submittedAt, "submittedAt");
        requireNonNull(createdAt, "createdAt");

        return new ServiceRequest(
            serviceRequestId,
            passportId,
            serviceType,
            ownerUserId,
            providerTenantId,
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

}

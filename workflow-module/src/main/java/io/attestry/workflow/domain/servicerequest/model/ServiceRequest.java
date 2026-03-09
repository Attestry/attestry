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
            ServiceRequestStatus.PENDING,
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

    public ServiceRequest accept(String serviceType, String description, Instant now) {
        requireText(serviceType, "serviceType");
        requireNonNull(now, "now");
        if (status != ServiceRequestStatus.PENDING) {
            throw new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE,
                "Only PENDING service request can be accepted");
        }
        return new ServiceRequest(
            serviceRequestId,
            passportId,
            serviceType,
            ownerUserId,
            providerTenantId,
            ServiceRequestStatus.ACCEPTED,
            description,
            beforeEvidenceGroupId,
            afterEvidenceGroupId,
            permissionId,
            submittedByUserId,
            submittedAt,
            completedAt,
            completedByUserId,
            cancelledAt,
            cancelReason,
            createdAt
        );
    }

    public ServiceRequest reject(String rejectReason, Instant now) {
        requireNonNull(now, "now");
        if (status != ServiceRequestStatus.PENDING) {
            throw new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE,
                "Only PENDING service request can be rejected");
        }
        return new ServiceRequest(
            serviceRequestId,
            passportId,
            serviceType,
            ownerUserId,
            providerTenantId,
            ServiceRequestStatus.REJECTED,
            description,
            beforeEvidenceGroupId,
            afterEvidenceGroupId,
            permissionId,
            submittedByUserId,
            submittedAt,
            completedAt,
            completedByUserId,
            now,
            rejectReason,
            createdAt
        );
    }

    public ServiceRequest complete(String completedByUserId, String afterEvidenceGroupId, Instant now) {
        requireText(completedByUserId, "completedByUserId");
        requireNonNull(now, "now");
        if (status != ServiceRequestStatus.ACCEPTED) {
            throw new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE,
                "Only ACCEPTED service request can be completed");
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
        if (status != ServiceRequestStatus.PENDING && status != ServiceRequestStatus.ACCEPTED) {
            throw new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE,
                "Only PENDING or ACCEPTED service request can be cancelled");
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

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
    String serviceRequestMethod,
    String symptomDescription,
    Instant requestedReservationAt,
    String contactMemo,
    String beforeEvidenceGroupId,
    String afterEvidenceGroupId,
    String serviceResultDetail,
    String completionMemo,
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
        String serviceRequestMethod,
        String symptomDescription,
        Instant requestedReservationAt,
        String contactMemo,
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
        requireText(beforeEvidenceGroupId, "beforeEvidenceGroupId");
        requireText(symptomDescription, "symptomDescription");
        requireText(contactMemo, "contactMemo");
        String normalizedRequestMethod = ServiceRequestMethods.normalize(serviceRequestMethod);
        if (ServiceRequestMethods.VISIT.equals(normalizedRequestMethod)) {
            requireNonNull(requestedReservationAt, "requestedReservationAt");
        }

        return new ServiceRequest(
            serviceRequestId,
            passportId,
            serviceType,
            ownerUserId,
            providerTenantId,
            ServiceRequestStatus.PENDING,
            description,
            normalizedRequestMethod,
            symptomDescription,
            requestedReservationAt,
            contactMemo,
            beforeEvidenceGroupId,
            null,
            null,
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
        String normalizedServiceType = ServiceTypes.normalize(serviceType);
        requireNonNull(now, "now");
        if (status != ServiceRequestStatus.PENDING) {
            throw new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE,
                "Only PENDING service request can be accepted");
        }
        return new ServiceRequest(
            serviceRequestId,
            passportId,
            normalizedServiceType,
            ownerUserId,
            providerTenantId,
            ServiceRequestStatus.ACCEPTED,
            description,
            serviceRequestMethod,
            symptomDescription,
            requestedReservationAt,
            contactMemo,
            beforeEvidenceGroupId,
            afterEvidenceGroupId,
            serviceResultDetail,
            completionMemo,
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
            serviceRequestMethod,
            symptomDescription,
            requestedReservationAt,
            contactMemo,
            beforeEvidenceGroupId,
            afterEvidenceGroupId,
            serviceResultDetail,
            completionMemo,
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

    public ServiceRequest complete(
        String completedByUserId,
        String serviceType,
        String afterEvidenceGroupId,
        String serviceResultDetail,
        String completionMemo,
        Instant now
    ) {
        requireText(completedByUserId, "completedByUserId");
        String normalizedServiceType = ServiceTypes.normalize(serviceType);
        requireText(afterEvidenceGroupId, "afterEvidenceGroupId");
        requireText(serviceResultDetail, "serviceResultDetail");
        requireText(completionMemo, "completionMemo");
        requireNonNull(now, "now");
        if (status != ServiceRequestStatus.ACCEPTED) {
            throw new WorkflowDomainException(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE,
                "Only ACCEPTED service request can be completed");
        }
        return new ServiceRequest(
            serviceRequestId,
            passportId,
            normalizedServiceType,
            ownerUserId,
            providerTenantId,
            ServiceRequestStatus.COMPLETED,
            description,
            serviceRequestMethod,
            symptomDescription,
            requestedReservationAt,
            contactMemo,
            beforeEvidenceGroupId,
            afterEvidenceGroupId,
            serviceResultDetail,
            completionMemo,
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
            serviceRequestMethod,
            symptomDescription,
            requestedReservationAt,
            contactMemo,
            beforeEvidenceGroupId,
            afterEvidenceGroupId,
            serviceResultDetail,
            completionMemo,
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

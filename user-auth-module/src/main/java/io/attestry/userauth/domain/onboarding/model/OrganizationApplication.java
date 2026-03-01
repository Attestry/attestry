package io.attestry.userauth.domain.onboarding.model;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.organization.model.GroupType;
import java.time.Instant;
import java.util.UUID;

public record OrganizationApplication(
    String applicationId,
    GroupType type,
    String applicantUserId,
    String tenantId,
    String orgName,
    String country,
    String bizRegNo,
    String evidenceBundleId,
    ApplicationStatus status,
    String reviewedByAdminId,
    Instant reviewedAt,
    String rejectReason
) {
    public static OrganizationApplication createBrand(
        String applicantUserId,
        String orgName,
        String country,
        String bizRegNo,
        String evidenceBundleId
    ) {
        validateEvidenceBundleId(evidenceBundleId);
        return new OrganizationApplication(
            UUID.randomUUID().toString(),
            GroupType.BRAND,
            applicantUserId,
            null,
            orgName,
            country,
            bizRegNo,
            evidenceBundleId,
            ApplicationStatus.PENDING,
            null,
            null,
            null
        );
    }

    public static OrganizationApplication createRetail(
        String applicantUserId,
        String orgName,
        String country,
        String bizRegNo,
        String evidenceBundleId
    ) {
        validateEvidenceBundleId(evidenceBundleId);
        return new OrganizationApplication(
            UUID.randomUUID().toString(),
            GroupType.RETAIL,
            applicantUserId,
            null,
            orgName,
            country,
            bizRegNo,
            evidenceBundleId,
            ApplicationStatus.PENDING,
            null,
            null,
            null
        );
    }

    public OrganizationApplication approve(String reviewerUserId, String tenantId, Instant reviewedAt) {
        assertPending();
        return new OrganizationApplication(
            applicationId,
            type,
            applicantUserId,
            tenantId,
            orgName,
            country,
            bizRegNo,
            evidenceBundleId,
            ApplicationStatus.APPROVED,
            reviewerUserId,
            reviewedAt,
            null
        );
    }

    public OrganizationApplication reject(String reviewerUserId, String reason, Instant reviewedAt) {
        assertPending();
        return new OrganizationApplication(
            applicationId,
            type,
            applicantUserId,
            tenantId,
            orgName,
            country,
            bizRegNo,
            evidenceBundleId,
            ApplicationStatus.REJECTED,
            reviewerUserId,
            reviewedAt,
            reason
        );
    }

    public void assertPending() {
        if (status != ApplicationStatus.PENDING) {
            throw new DomainException(ErrorCode.INVALID_APPLICATION_STATE, "Application is not pending");
        }
    }

    private static void validateEvidenceBundleId(String evidenceBundleId) {
        if (evidenceBundleId == null || evidenceBundleId.isBlank()) {
            throw new DomainException(ErrorCode.INVALID_APPLICATION_STATE, "Evidence bundle id is required");
        }
    }
}

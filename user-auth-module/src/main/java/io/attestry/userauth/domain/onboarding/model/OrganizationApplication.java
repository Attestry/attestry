package io.attestry.userauth.domain.onboarding.model;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.organization.model.GroupType;
import java.time.Instant;
import java.util.UUID;

public class OrganizationApplication {

    private final String applicationId;
    private final GroupType type;
    private final String applicantUserId;
    private String tenantId;
    private final String orgName;
    private final String country;
    private final String bizRegNo;
    private final String evidenceBundleId;
    private ApplicationStatus status;
    private String reviewedByAdminId;
    private Instant reviewedAt;
    private String rejectReason;

    private OrganizationApplication(String applicationId, GroupType type, String applicantUserId,
                                     String tenantId, String orgName, String country, String bizRegNo,
                                     String evidenceBundleId, ApplicationStatus status,
                                     String reviewedByAdminId, Instant reviewedAt, String rejectReason) {
        this.applicationId = applicationId;
        this.type = type;
        this.applicantUserId = applicantUserId;
        this.tenantId = tenantId;
        this.orgName = orgName;
        this.country = country;
        this.bizRegNo = bizRegNo;
        this.evidenceBundleId = evidenceBundleId;
        this.status = status;
        this.reviewedByAdminId = reviewedByAdminId;
        this.reviewedAt = reviewedAt;
        this.rejectReason = rejectReason;
    }

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

    public static OrganizationApplication reconstitute(
        String applicationId, GroupType type, String applicantUserId,
        String tenantId, String orgName, String country, String bizRegNo,
        String evidenceBundleId, ApplicationStatus status,
        String reviewedByAdminId, Instant reviewedAt, String rejectReason
    ) {
        return new OrganizationApplication(
            applicationId, type, applicantUserId, tenantId, orgName, country, bizRegNo,
            evidenceBundleId, status, reviewedByAdminId, reviewedAt, rejectReason
        );
    }

    public void approve(String reviewerUserId, String assignedTenantId, Instant now) {
        assertPending();
        this.status = ApplicationStatus.APPROVED;
        this.tenantId = assignedTenantId;
        this.reviewedByAdminId = reviewerUserId;
        this.reviewedAt = now;
    }

    public void reject(String reviewerUserId, String reason, Instant now) {
        assertPending();
        this.status = ApplicationStatus.REJECTED;
        this.reviewedByAdminId = reviewerUserId;
        this.reviewedAt = now;
        this.rejectReason = reason;
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

    // Getters
    public String applicationId() { return applicationId; }
    public GroupType type() { return type; }
    public String applicantUserId() { return applicantUserId; }
    public String tenantId() { return tenantId; }
    public String orgName() { return orgName; }
    public String country() { return country; }
    public String bizRegNo() { return bizRegNo; }
    public String evidenceBundleId() { return evidenceBundleId; }
    public ApplicationStatus status() { return status; }
    public String reviewedByAdminId() { return reviewedByAdminId; }
    public Instant reviewedAt() { return reviewedAt; }
    public String rejectReason() { return rejectReason; }
}

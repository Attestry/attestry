package io.attestry.userauth.domain.onboarding.model;

import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.tenant.model.TenantType;
import java.time.Instant;
import java.util.UUID;

public class OrganizationApplication {

    private final String applicationId;
    private final TenantType type;
    private final String applicantUserId;
    private String tenantId;
    private final String orgName;
    private final String country;
    private final String address;
    private final String bizRegNo;
    private final String evidenceBundleId;
    private ApplicationStatus status;
    private String reviewedByAdminId;
    private Instant reviewedAt;
    private String rejectReason;

    private OrganizationApplication(
        String applicationId,
        TenantType type,
        String applicantUserId,
        String tenantId,
        String orgName,
        String country,
        String address,
        String bizRegNo,
        String evidenceBundleId,
        ApplicationStatus status,
        String reviewedByAdminId,
        Instant reviewedAt,
        String rejectReason
    ) {
        this.applicationId = applicationId;
        this.type = type;
        this.applicantUserId = applicantUserId;
        this.tenantId = tenantId;
        this.orgName = orgName;
        this.country = country;
        this.address = address;
        this.bizRegNo = bizRegNo;
        this.evidenceBundleId = evidenceBundleId;
        this.status = status;
        this.reviewedByAdminId = reviewedByAdminId;
        this.reviewedAt = reviewedAt;
        this.rejectReason = rejectReason;
    }

    public static OrganizationApplication create(
        TenantType type,
        String applicantUserId,
        String orgName,
        String country,
        String address,
        String bizRegNo,
        String evidenceBundleId
    ) {
        validateEvidenceBundleId(evidenceBundleId);
        if (type == TenantType.SERVICE) {
            validateAddress(address);
        }
        return new OrganizationApplication(
            UUID.randomUUID().toString(),
            type,
            applicantUserId,
            null,
            orgName,
            country,
            address,
            bizRegNo,
            evidenceBundleId,
            ApplicationStatus.PENDING,
            null,
            null,
            null
        );
    }

    public static OrganizationApplication reconstitute(
        String applicationId,
        TenantType type,
        String applicantUserId,
        String tenantId,
        String orgName,
        String country,
        String address,
        String bizRegNo,
        String evidenceBundleId,
        ApplicationStatus status,
        String reviewedByAdminId,
        Instant reviewedAt,
        String rejectReason
    ) {
        return new OrganizationApplication(
            applicationId,
            type,
            applicantUserId,
            tenantId,
            orgName,
            country,
            address,
            bizRegNo,
            evidenceBundleId,
            status,
            reviewedByAdminId,
            reviewedAt,
            rejectReason
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
            throw new UserAuthDomainException(
                UserAuthErrorCode.INVALID_APPLICATION_STATE,
                "Application is not pending"
            );
        }
    }

    private static void validateEvidenceBundleId(String evidenceBundleId) {
        if (evidenceBundleId == null || evidenceBundleId.isBlank()) {
            throw new UserAuthDomainException(
                UserAuthErrorCode.INVALID_APPLICATION_STATE,
                "Evidence bundle id is required"
            );
        }
    }

    private static void validateAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new UserAuthDomainException(
                UserAuthErrorCode.INVALID_REQUEST,
                "address is required"
            );
        }
    }

    public String applicationId() {
        return applicationId;
    }

    public TenantType type() {
        return type;
    }

    public String applicantUserId() {
        return applicantUserId;
    }

    public String tenantId() {
        return tenantId;
    }

    public String orgName() {
        return orgName;
    }

    public String country() {
        return country;
    }

    public String address() {
        return address;
    }

    public String bizRegNo() {
        return bizRegNo;
    }

    public String evidenceBundleId() {
        return evidenceBundleId;
    }

    public ApplicationStatus status() {
        return status;
    }

    public String reviewedByAdminId() {
        return reviewedByAdminId;
    }

    public Instant reviewedAt() {
        return reviewedAt;
    }

    public String rejectReason() {
        return rejectReason;
    }
}

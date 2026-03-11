package io.attestry.userauth.domain.onboarding.model;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.organization.model.TenantType;
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
    private java.time.Instant reviewedAt;
    private String rejectReason;

    private OrganizationApplication(String applicationId, TenantType type, String applicantUserId,
            String tenantId, String orgName, String country, String address, String bizRegNo,
            String evidenceBundleId, ApplicationStatus status,
            String reviewedByAdminId, java.time.Instant reviewedAt, String rejectReason) {
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

    public static OrganizationApplication createBrand(
            String applicantUserId,
            String orgName,
            String country,
            String address,
            String bizRegNo,
            String evidenceBundleId) {
        validateEvidenceBundleId(evidenceBundleId);
        return new OrganizationApplication(
                UUID.randomUUID().toString(),
                TenantType.BRAND,
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
                null);
    }

    public static OrganizationApplication createService(
            String applicantUserId,
            String orgName,
            String country,
            String address,
            String bizRegNo,
            String evidenceBundleId) {
        validateEvidenceBundleId(evidenceBundleId);
        validateAddress(address);
        return new OrganizationApplication(
                UUID.randomUUID().toString(),
                TenantType.SERVICE,
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
                null);
    }

    public static OrganizationApplication createRetail(
            String applicantUserId,
            String orgName,
            String country,
            String address,
            String bizRegNo,
            String evidenceBundleId) {
        validateEvidenceBundleId(evidenceBundleId);
        return new OrganizationApplication(
                UUID.randomUUID().toString(),
                TenantType.RETAIL,
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
                null);
    }

    public static OrganizationApplication reconstitute(
            String applicationId, TenantType type, String applicantUserId,
            String tenantId, String orgName, String country, String address, String bizRegNo,
            String evidenceBundleId, ApplicationStatus status,
            String reviewedByAdminId, java.time.Instant reviewedAt, String rejectReason) {
        return new OrganizationApplication(
                applicationId, type, applicantUserId, tenantId, orgName, country, address, bizRegNo,
                evidenceBundleId, status, reviewedByAdminId, reviewedAt, rejectReason);
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

    private static void validateAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "address is required");
        }
    }

    // Getters
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

    public java.time.Instant reviewedAt() {
        return reviewedAt;
    }

    public String rejectReason() {
        return rejectReason;
    }
}

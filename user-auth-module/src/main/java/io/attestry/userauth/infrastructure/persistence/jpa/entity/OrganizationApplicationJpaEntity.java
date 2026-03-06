package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import io.attestry.userauth.domain.onboarding.model.ApplicationStatus;
import io.attestry.userauth.domain.organization.model.TenantType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "organization_applications")
public class OrganizationApplicationJpaEntity {

    @Id
    @Column(name = "application_id", nullable = false, length = 36)
    private String applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TenantType type;

    @Column(name = "applicant_user_id", nullable = false, length = 36)
    private String applicantUserId;

    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @Column(name = "org_name", nullable = false)
    private String orgName;

    @Column(name = "country", nullable = false, length = 10)
    private String country;

    @Column(name = "biz_reg_no")
    private String bizRegNo;

    @Column(name = "evidence_bundle_id", nullable = false, length = 36)
    private String evidenceBundleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;

    @Column(name = "reviewed_by_admin_id", length = 36)
    private String reviewedByAdminId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reject_reason", length = 1000)
    private String rejectReason;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected OrganizationApplicationJpaEntity() {
    }

    public OrganizationApplicationJpaEntity(
        String applicationId,
        TenantType type,
        String applicantUserId,
        String tenantId,
        String orgName,
        String country,
        String bizRegNo,
        String evidenceBundleId,
        ApplicationStatus status,
        String reviewedByAdminId,
        Instant reviewedAt,
        String rejectReason,
        long rowVersion
    ) {
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
        this.rowVersion = rowVersion;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public TenantType getType() {
        return type;
    }

    public String getApplicantUserId() {
        return applicantUserId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getOrgName() {
        return orgName;
    }

    public String getCountry() {
        return country;
    }

    public String getBizRegNo() {
        return bizRegNo;
    }

    public String getEvidenceBundleId() {
        return evidenceBundleId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public String getReviewedByAdminId() {
        return reviewedByAdminId;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public long getRowVersion() {
        return rowVersion;
    }
}

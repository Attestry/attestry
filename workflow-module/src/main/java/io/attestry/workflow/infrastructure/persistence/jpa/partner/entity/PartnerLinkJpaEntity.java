package io.attestry.workflow.infrastructure.persistence.jpa.partner.entity;

import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.model.PartnerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "partner_links")
public class PartnerLinkJpaEntity {

    @Id
    @Column(name = "partner_link_id", nullable = false, length = 36)
    private String partnerLinkId;

    @Column(name = "source_tenant_id", nullable = false, length = 36)
    private String sourceTenantId;

    @Column(name = "target_tenant_id", nullable = false, length = 36)
    private String targetTenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_type", nullable = false, length = 30)
    private PartnerType partnerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PartnerLinkStatus status;

    @Column(name = "created_by_user_id", nullable = false, length = 36)
    private String createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "approved_by_user_id", length = 36)
    private String approvedByUserId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected PartnerLinkJpaEntity() {
    }

    public PartnerLinkJpaEntity(
        String partnerLinkId,
        String sourceTenantId,
        String targetTenantId,
        PartnerType partnerType,
        PartnerLinkStatus status,
        String createdByUserId,
        Instant createdAt,
        String approvedByUserId,
        Instant approvedAt,
        Instant expiresAt,
        Instant terminatedAt,
        String reason,
        long rowVersion
    ) {
        this.partnerLinkId = partnerLinkId;
        this.sourceTenantId = sourceTenantId;
        this.targetTenantId = targetTenantId;
        this.partnerType = partnerType;
        this.status = status;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
        this.approvedByUserId = approvedByUserId;
        this.approvedAt = approvedAt;
        this.expiresAt = expiresAt;
        this.terminatedAt = terminatedAt;
        this.reason = reason;
        this.rowVersion = rowVersion;
    }

    public String getPartnerLinkId() {
        return partnerLinkId;
    }

    public String getSourceTenantId() {
        return sourceTenantId;
    }

    public String getTargetTenantId() {
        return targetTenantId;
    }

    // Backward compatibility for transitional usages.
    public String getBrandTenantId() {
        return sourceTenantId;
    }

    public String getPartnerTenantId() {
        return targetTenantId;
    }

    public PartnerType getPartnerType() {
        return partnerType;
    }

    public PartnerLinkStatus getStatus() {
        return status;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getApprovedByUserId() {
        return approvedByUserId;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getTerminatedAt() {
        return terminatedAt;
    }

    public String getReason() {
        return reason;
    }

    public long getRowVersion() {
        return rowVersion;
    }
}

package io.attestry.workflow.infrastructure.persistence.jpa.entity;

import io.attestry.workflow.domain.distribution.model.DistributionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "distributions")
public class DistributionJpaEntity {

    @Id
    @Column(name = "distribution_id", nullable = false, length = 36)
    private String distributionId;

    @Column(name = "passport_id", nullable = false, length = 36)
    private String passportId;

    @Column(name = "source_tenant_id", nullable = false, length = 36)
    private String sourceTenantId;

    @Column(name = "target_tenant_id", nullable = false, length = 36)
    private String targetTenantId;

    @Column(name = "partner_link_id", nullable = false, length = 36)
    private String partnerLinkId;

    @Column(name = "delegation_id", nullable = false, length = 36)
    private String delegationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DistributionStatus status;

    @Column(name = "distributed_by_user_id", nullable = false, length = 36)
    private String distributedByUserId;

    @Column(name = "distributed_at", nullable = false)
    private Instant distributedAt;

    @Column(name = "recalled_by_user_id", length = 36)
    private String recalledByUserId;

    @Column(name = "recalled_at")
    private Instant recalledAt;

    @Column(name = "recall_reason", length = 1000)
    private String recallReason;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected DistributionJpaEntity() {
    }

    public DistributionJpaEntity(
        String distributionId,
        String passportId,
        String sourceTenantId,
        String targetTenantId,
        String partnerLinkId,
        String delegationId,
        DistributionStatus status,
        String distributedByUserId,
        Instant distributedAt,
        String recalledByUserId,
        Instant recalledAt,
        String recallReason,
        long rowVersion
    ) {
        this.distributionId = distributionId;
        this.passportId = passportId;
        this.sourceTenantId = sourceTenantId;
        this.targetTenantId = targetTenantId;
        this.partnerLinkId = partnerLinkId;
        this.delegationId = delegationId;
        this.status = status;
        this.distributedByUserId = distributedByUserId;
        this.distributedAt = distributedAt;
        this.recalledByUserId = recalledByUserId;
        this.recalledAt = recalledAt;
        this.recallReason = recallReason;
        this.rowVersion = rowVersion;
    }

    public String getDistributionId() { return distributionId; }
    public String getPassportId() { return passportId; }
    public String getSourceTenantId() { return sourceTenantId; }
    public String getTargetTenantId() { return targetTenantId; }
    public String getPartnerLinkId() { return partnerLinkId; }
    public String getDelegationId() { return delegationId; }
    public DistributionStatus getStatus() { return status; }
    public String getDistributedByUserId() { return distributedByUserId; }
    public Instant getDistributedAt() { return distributedAt; }
    public String getRecalledByUserId() { return recalledByUserId; }
    public Instant getRecalledAt() { return recalledAt; }
    public String getRecallReason() { return recallReason; }
    public long getRowVersion() { return rowVersion; }
}

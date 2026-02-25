package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import io.attestry.userauth.domain.approval.model.ApprovalStatus;
import io.attestry.userauth.domain.approval.model.ApprovalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "approval_requests")
public class ApprovalRequestJpaEntity {

    @Id
    @Column(name = "approval_id", nullable = false, length = 36)
    private String approvalId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ApprovalType type;

    @Column(name = "target_id", nullable = false, length = 36)
    private String targetId;

    @Column(name = "payload")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApprovalStatus status;

    @Column(name = "requested_by", nullable = false, length = 36)
    private String requestedBy;

    @Column(name = "approved_by", length = 36)
    private String approvedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected ApprovalRequestJpaEntity() {
    }

    public ApprovalRequestJpaEntity(
        String approvalId,
        String tenantId,
        ApprovalType type,
        String targetId,
        String payload,
        ApprovalStatus status,
        String requestedBy,
        String approvedBy,
        Instant createdAt,
        Instant decidedAt
    ) {
        this.approvalId = approvalId;
        this.tenantId = tenantId;
        this.type = type;
        this.targetId = targetId;
        this.payload = payload;
        this.status = status;
        this.requestedBy = requestedBy;
        this.approvedBy = approvedBy;
        this.createdAt = createdAt;
        this.decidedAt = decidedAt;
    }

    public String getApprovalId() {
        return approvalId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public ApprovalType getType() {
        return type;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getPayload() {
        return payload;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}

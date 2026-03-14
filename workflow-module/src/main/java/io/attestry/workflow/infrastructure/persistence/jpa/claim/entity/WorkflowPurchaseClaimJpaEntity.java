package io.attestry.workflow.infrastructure.persistence.jpa.claim.entity;

import io.attestry.workflow.domain.claim.model.PurchaseClaimStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "purchase_claim_requests")
public class WorkflowPurchaseClaimJpaEntity {

    @Id
    @Column(name = "claim_id", nullable = false, length = 36)
    private String claimId;

    @Column(name = "claimant_user_id", nullable = false, length = 36)
    private String claimantUserId;

    @Column(name = "serial_number", nullable = false, length = 255)
    private String serialNumber;

    @Column(name = "model_name", nullable = false, length = 255)
    private String modelName;

    @Column(name = "evidence_group_id", nullable = false, length = 36)
    private String evidenceGroupId;

    @Column(name = "note", length = 2000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PurchaseClaimStatus status;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_by_user_id", length = 36)
    private String reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "passport_id", length = 36)
    private String passportId;

    @Column(name = "asset_id", length = 36)
    private String assetId;

    protected WorkflowPurchaseClaimJpaEntity() {
    }

    public WorkflowPurchaseClaimJpaEntity(
        String claimId,
        String claimantUserId,
        String serialNumber,
        String modelName,
        String evidenceGroupId,
        String note,
        PurchaseClaimStatus status,
        Instant submittedAt,
        String reviewedByUserId,
        Instant reviewedAt,
        String rejectionReason,
        String passportId,
        String assetId
    ) {
        this.claimId = claimId;
        this.claimantUserId = claimantUserId;
        this.serialNumber = serialNumber;
        this.modelName = modelName;
        this.evidenceGroupId = evidenceGroupId;
        this.note = note;
        this.status = status;
        this.submittedAt = submittedAt;
        this.reviewedByUserId = reviewedByUserId;
        this.reviewedAt = reviewedAt;
        this.rejectionReason = rejectionReason;
        this.passportId = passportId;
        this.assetId = assetId;
    }

    public String getClaimId() { return claimId; }
    public String getClaimantUserId() { return claimantUserId; }
    public String getSerialNumber() { return serialNumber; }
    public String getModelName() { return modelName; }
    public String getEvidenceGroupId() { return evidenceGroupId; }
    public String getNote() { return note; }
    public PurchaseClaimStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public String getReviewedByUserId() { return reviewedByUserId; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public String getPassportId() { return passportId; }
    public String getAssetId() { return assetId; }
}

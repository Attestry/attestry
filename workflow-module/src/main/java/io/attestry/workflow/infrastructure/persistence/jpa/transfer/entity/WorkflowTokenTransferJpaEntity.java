package io.attestry.workflow.infrastructure.persistence.jpa.transfer.entity;

import io.attestry.workflow.domain.transfer.model.AcceptMethod;
import io.attestry.workflow.domain.transfer.model.TransferStatus;
import io.attestry.workflow.domain.transfer.model.TransferType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "token_transfers")
public class WorkflowTokenTransferJpaEntity {

    @Id
    @Column(name = "transfer_id", nullable = false, length = 36)
    private String transferId;

    @Column(name = "passport_id", nullable = false, length = 36)
    private String passportId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false, length = 10)
    private TransferType transferType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "accept_method", nullable = false, length = 10)
    private AcceptMethod acceptMethod;

    @Column(name = "from_owner_id", length = 36)
    private String fromOwnerId;

    @Column(name = "to_owner_id", length = 36)
    private String toOwnerId;

    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @Column(name = "qr_nonce", length = 64)
    private String qrNonce;

    @Column(name = "code_hash", length = 64)
    private String codeHash;

    @Column(name = "code_salt", length = 36)
    private String codeSalt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by_user_id", nullable = false, length = 36)
    private String createdByUserId;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by_user_id", length = 36)
    private String cancelledByUserId;

    protected WorkflowTokenTransferJpaEntity() {
    }

    public WorkflowTokenTransferJpaEntity(
        String transferId,
        String passportId,
        TransferType transferType,
        TransferStatus status,
        AcceptMethod acceptMethod,
        String fromOwnerId,
        String toOwnerId,
        String tenantId,
        String qrNonce,
        String codeHash,
        String codeSalt,
        int attemptCount,
        Instant expiresAt,
        Instant createdAt,
        String createdByUserId,
        Instant completedAt,
        Instant cancelledAt,
        String cancelledByUserId
    ) {
        this.transferId = transferId;
        this.passportId = passportId;
        this.transferType = transferType;
        this.status = status;
        this.acceptMethod = acceptMethod;
        this.fromOwnerId = fromOwnerId;
        this.toOwnerId = toOwnerId;
        this.tenantId = tenantId;
        this.qrNonce = qrNonce;
        this.codeHash = codeHash;
        this.codeSalt = codeSalt;
        this.attemptCount = attemptCount;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.createdByUserId = createdByUserId;
        this.completedAt = completedAt;
        this.cancelledAt = cancelledAt;
        this.cancelledByUserId = cancelledByUserId;
    }

    public String getTransferId() { return transferId; }
    public String getPassportId() { return passportId; }
    public TransferType getTransferType() { return transferType; }
    public TransferStatus getStatus() { return status; }
    public AcceptMethod getAcceptMethod() { return acceptMethod; }
    public String getFromOwnerId() { return fromOwnerId; }
    public String getToOwnerId() { return toOwnerId; }
    public String getTenantId() { return tenantId; }
    public String getQrNonce() { return qrNonce; }
    public String getCodeHash() { return codeHash; }
    public String getCodeSalt() { return codeSalt; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedByUserId() { return createdByUserId; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public String getCancelledByUserId() { return cancelledByUserId; }
}

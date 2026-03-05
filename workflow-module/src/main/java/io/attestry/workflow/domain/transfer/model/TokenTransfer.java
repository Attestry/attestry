package io.attestry.workflow.domain.transfer.model;

import static io.attestry.workflow.domain.WorkflowValidation.requireNonNull;
import static io.attestry.workflow.domain.WorkflowValidation.requireText;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Instant;

public record TokenTransfer(
    String transferId,
    String passportId,
    TransferType transferType,
    TransferStatus status,
    AcceptMethod acceptMethod,
    String fromOwnerId,
    String toOwnerId,
    String tenantId,
    String groupId,
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

    private static final int MAX_ATTEMPT_COUNT = 5;

    public static TokenTransfer createC2C(
        String transferId,
        String passportId,
        String fromOwnerId,
        AcceptCredential credential,
        Instant expiresAt,
        Instant createdAt,
        String createdByUserId
    ) {
        requireText(transferId, "transferId");
        requireText(passportId, "passportId");
        requireText(fromOwnerId, "fromOwnerId");
        requireText(createdByUserId, "createdByUserId");
        requireNonNull(credential, "credential");
        requireNonNull(expiresAt, "expiresAt");
        requireNonNull(createdAt, "createdAt");

        return new TokenTransfer(
            transferId, passportId, TransferType.C2C, TransferStatus.PENDING, credential.method(),
            fromOwnerId, null, null, null,
            credential.qrNonce(), credential.codeHash(), credential.codeSalt(),
            0, expiresAt, createdAt, createdByUserId,
            null, null, null
        );
    }

    public static TokenTransfer createB2C(
        String transferId,
        String passportId,
        String tenantId,
        String groupId,
        AcceptCredential credential,
        Instant expiresAt,
        Instant createdAt,
        String createdByUserId
    ) {
        requireText(transferId, "transferId");
        requireText(passportId, "passportId");
        requireText(tenantId, "tenantId");
        requireText(groupId, "groupId");
        requireText(createdByUserId, "createdByUserId");
        requireNonNull(credential, "credential");
        requireNonNull(expiresAt, "expiresAt");
        requireNonNull(createdAt, "createdAt");

        return new TokenTransfer(
            transferId, passportId, TransferType.B2C, TransferStatus.PENDING, credential.method(),
            null, null, tenantId, groupId,
            credential.qrNonce(), credential.codeHash(), credential.codeSalt(),
            0, expiresAt, createdAt, createdByUserId,
            null, null, null
        );
    }

    public TokenTransfer complete(String toOwnerId, Instant now) {
        requireText(toOwnerId, "toOwnerId");
        requireNonNull(now, "now");
        if (status != TransferStatus.PENDING) {
            throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_INVALID_STATE,
                "Only PENDING transfer can be completed");
        }
        if (isExpired(now)) {
            throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_EXPIRED,
                "Transfer has expired");
        }
        return new TokenTransfer(
            transferId, passportId, transferType, TransferStatus.COMPLETED, acceptMethod,
            fromOwnerId, toOwnerId, tenantId, groupId,
            qrNonce, codeHash, codeSalt,
            attemptCount, expiresAt, createdAt, createdByUserId,
            now, null, null
        );
    }

    public TokenTransfer cancel(String cancelledByUserId, Instant now) {
        requireNonNull(now, "now");
        if (status != TransferStatus.PENDING) {
            throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_INVALID_STATE,
                "Only PENDING transfer can be cancelled");
        }
        return new TokenTransfer(
            transferId, passportId, transferType, TransferStatus.CANCELLED, acceptMethod,
            fromOwnerId, toOwnerId, tenantId, groupId,
            qrNonce, codeHash, codeSalt,
            attemptCount, expiresAt, createdAt, createdByUserId,
            null, now, cancelledByUserId
        );
    }

    public TokenTransfer markExpired(Instant now) {
        requireNonNull(now, "now");
        if (status != TransferStatus.PENDING) {
            throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_INVALID_STATE,
                "Only PENDING transfer can be marked expired");
        }
        return new TokenTransfer(
            transferId, passportId, transferType, TransferStatus.EXPIRED, acceptMethod,
            fromOwnerId, toOwnerId, tenantId, groupId,
            qrNonce, codeHash, codeSalt,
            attemptCount, expiresAt, createdAt, createdByUserId,
            null, now, null
        );
    }

    public TokenTransfer incrementAttempt() {
        return new TokenTransfer(
            transferId, passportId, transferType, status, acceptMethod,
            fromOwnerId, toOwnerId, tenantId, groupId,
            qrNonce, codeHash, codeSalt,
            attemptCount + 1, expiresAt, createdAt, createdByUserId,
            completedAt, cancelledAt, cancelledByUserId
        );
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isBruteForceBlocked() {
        return attemptCount >= MAX_ATTEMPT_COUNT;
    }

    public boolean verifyQrNonce(String nonce) {
        return qrNonce != null && qrNonce.equals(nonce);
    }

}

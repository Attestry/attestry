package io.attestry.workflow.application.transfer.internal;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferStatus;
import io.attestry.workflow.domain.transfer.model.TransferType;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferLookupService {

    private final TokenTransferRepository transferRepository;

    public TokenTransfer getByIdOrThrow(String transferId) {
        return transferRepository.findById(transferId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.TRANSFER_NOT_FOUND, "Transfer not found"));
    }

    public TokenTransfer getPendingForAccept(String transferId, Instant now) {
        TokenTransfer transfer = getByIdOrThrow(transferId);

        if (transfer.status() != TransferStatus.PENDING) {
            throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_INVALID_STATE, "Transfer is not in PENDING state");
        }
        if (transfer.isExpired(now)) {
            throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_EXPIRED, "Transfer has expired");
        }
        if (transfer.isBruteForceBlocked()) {
            throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_BRUTE_FORCE_BLOCKED, "Too many failed attempts");
        }
        return transfer;
    }

    public TokenTransfer getPendingForCancel(String transferId) {
        TokenTransfer transfer = getByIdOrThrow(transferId);

        if (transfer.status() != TransferStatus.PENDING) {
            throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_INVALID_STATE, "Only PENDING transfer can be cancelled");
        }
        return transfer;
    }

    public Optional<TokenTransfer> findLatestActivePendingByPassportId(
        String passportId,
        Instant now,
        TransferType transferType,
        String tenantId
    ) {
        return transferRepository.findLatestActivePendingByPassportId(passportId, now, transferType, tenantId);
    }
}

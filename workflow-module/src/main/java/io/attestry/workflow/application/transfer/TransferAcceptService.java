package io.attestry.workflow.application.transfer;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.TransferLedgerOutboxPort;
import io.attestry.workflow.application.port.TransferOwnershipUpdatePort;
import io.attestry.workflow.application.port.TransferProductReadPort;
import io.attestry.workflow.application.shipment.result.WorkflowLedgerEventEnvelope;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.transfer.command.AcceptTransferCommand;
import io.attestry.workflow.application.transfer.result.AcceptTransferResult;
import io.attestry.workflow.application.usecase.TransferAcceptUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.transfer.model.AcceptMethod;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferStatus;
import io.attestry.workflow.domain.transfer.model.TransferType;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferAcceptService implements TransferAcceptUseCase {

    private final TokenTransferRepository transferRepository;
    private final TransferProductReadPort productReadPort;
    private final TransferOwnershipUpdatePort ownershipUpdatePort;
    private final TransferLedgerOutboxPort outboxPort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final TransferHashSupport hashSupport;
    private final Clock clock;

    public TransferAcceptService(
        TokenTransferRepository transferRepository,
        TransferProductReadPort productReadPort,
        TransferOwnershipUpdatePort ownershipUpdatePort,
        TransferLedgerOutboxPort outboxPort,
        WorkflowAuthorizationSupport authorizationSupport,
        TransferHashSupport hashSupport,
        Clock clock
    ) {
        this.transferRepository = transferRepository;
        this.productReadPort = productReadPort;
        this.ownershipUpdatePort = ownershipUpdatePort;
        this.outboxPort = outboxPort;
        this.authorizationSupport = authorizationSupport;
        this.hashSupport = hashSupport;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AcceptTransferResult accept(
        AuthPrincipal principal,
        String transferId,
        AcceptTransferCommand command
    ) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_TRANSFER_ACCEPT, "transfer:accept:" + transferId);

        Instant now = Instant.now(clock);

        TokenTransfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.TRANSFER_NOT_FOUND, "Transfer not found"));

        if (transfer.status() != TransferStatus.PENDING) {
            throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_INVALID_STATE, "Transfer is not in PENDING state");
        }
        if (transfer.isExpired(now)) {
            throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_EXPIRED, "Transfer has expired");
        }
        if (transfer.isBruteForceBlocked()) {
            throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_BRUTE_FORCE_BLOCKED, "Too many failed attempts");
        }

        TransferProductReadPort.TransferPassportState state = productReadPort.findPassportState(transfer.passportId())
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport not found"));
        if (!"NONE".equals(state.riskFlag())) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_STATE, "Risk flagged passport cannot be transferred");
        }

        if (transfer.transferType() == TransferType.C2C) {
            String currentOwnerId = productReadPort.findCurrentOwnerId(transfer.passportId())
                .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_STATE, "Passport ownership changed during transfer"));
            if (!currentOwnerId.equals(transfer.fromOwnerId())) {
                throw new WorkflowDomainException(WorkflowErrorCode.INVALID_STATE, "Passport ownership changed during transfer");
            }
        }

        if (transfer.acceptMethod() == AcceptMethod.QR) {
            if (!transfer.verifyQrNonce(command.qrNonce())) {
                throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_NONCE_MISMATCH, "QR nonce does not match");
            }
        } else {
            transfer = transfer.incrementAttempt();
            transferRepository.save(transfer);

            if (!hashSupport.verify(command.password(), transfer.codeHash(), transfer.codeSalt())) {
                if (transfer.isBruteForceBlocked()) {
                    TokenTransfer autoCancelled = transfer.cancel(null, now);
                    transferRepository.save(autoCancelled);
                }
                throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_CODE_MISMATCH, "Password does not match");
            }
        }

        TokenTransfer completed = transfer.complete(principal.userId(), now);
        TokenTransfer saved = transferRepository.save(completed);
        ownershipUpdatePort.upsertOwner(saved.passportId(), saved.toOwnerId(), now);

        WorkflowLedgerEventEnvelope envelope = saved.transferType() == TransferType.B2C
            ? WorkflowLedgerEventEnvelope.ownershipClaimed(saved)
            : WorkflowLedgerEventEnvelope.ownershipTransferCompleted(saved);

        String outboxEventId = outboxPort.enqueue(envelope);

        return new AcceptTransferResult(
            saved.transferId(), saved.passportId(),
            saved.status().name(), saved.toOwnerId(),
            saved.completedAt(), outboxEventId
        );
    }
}

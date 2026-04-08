package io.attestry.workflow.application.transfer.internal;

import io.attestry.workflow.application.port.transfer.TransferOwnershipUpdatePort;
import io.attestry.workflow.application.port.common.WorkflowLedgerOutboxPort;
import io.attestry.commonlib.outbox.OutboxEventEnvelope;
import io.attestry.workflow.application.event.WorkflowLedgerEvents;
import io.attestry.workflow.application.delegation.command.DelegationLifecycleUseCase;
import io.attestry.workflow.application.transfer.command.AcceptTransferCommand;
import io.attestry.workflow.application.transfer.result.AcceptTransferResult;
import io.attestry.workflow.domain.transfer.model.AcceptMethod;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferType;
import io.attestry.workflow.domain.transfer.policy.TransferAcceptPolicy;
import io.attestry.workflow.domain.transfer.policy.TransferAcceptPolicy.TransferAcceptContext;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferAcceptExecutor {

    private final TokenTransferRepository transferRepository;
    private final TransferOwnershipUpdatePort ownershipUpdatePort;
    private final WorkflowLedgerOutboxPort outboxPort;
    private final DelegationLifecycleUseCase delegationLifecycleUseCase;
    private final TransferHashSupport hashSupport;
    private final TransferAcceptPolicy acceptPolicy;
    private final Clock clock;

    public AcceptTransferResult accept(
        String toOwnerId,
        AcceptTransferCommand command,
        TransferAcceptContext context,
        TokenTransfer transfer
    ) {
        Instant now = Instant.now(clock);
        acceptPolicy.assertAcceptable(context);
        verifyCredential(transfer, command, now);
        TokenTransfer completed = transferRepository.save(transfer.complete(toOwnerId, now));
        return applyPostAcceptEffects(completed, now);
    }

    private void verifyCredential(TokenTransfer transfer, AcceptTransferCommand command, Instant now) {
        if (transfer.acceptMethod() == AcceptMethod.QR) {
            if (!transfer.verifyQrNonce(command.qrNonce())) {
                throw new io.attestry.workflow.domain.WorkflowDomainException(
                    io.attestry.workflow.domain.WorkflowErrorCode.TRANSFER_NONCE_MISMATCH,
                    "QR nonce does not match"
                );
            }
            return;
        }

        TokenTransfer incremented = transfer.incrementAttempt();
        transferRepository.save(incremented);

        if (!hashSupport.verify(command.password(), incremented.codeHash(), incremented.codeSalt())) {
            if (incremented.isBruteForceBlocked()) {
                transferRepository.save(incremented.cancel(null, now));
            }
            throw new io.attestry.workflow.domain.WorkflowDomainException(
                io.attestry.workflow.domain.WorkflowErrorCode.TRANSFER_CODE_MISMATCH,
                "Password does not match"
            );
        }
    }

    private AcceptTransferResult applyPostAcceptEffects(TokenTransfer completed, Instant now) {
        ownershipUpdatePort.upsertOwner(completed.passportId(), completed.toOwnerId(), now);

        if (completed.transferType() == TransferType.B2C) {
            delegationLifecycleUseCase.consumeByPassportId(completed.passportId());
        }

        OutboxEventEnvelope envelope = completed.transferType() == TransferType.B2C
            ? WorkflowLedgerEvents.ownershipClaimed(completed)
            : WorkflowLedgerEvents.ownershipTransferCompleted(completed);

        String outboxEventId = outboxPort.enqueue(envelope);

        return new AcceptTransferResult(
            completed.transferId(),
            completed.passportId(),
            completed.status().name(),
            completed.toOwnerId(),
            completed.completedAt(),
            outboxEventId
        );
    }
}

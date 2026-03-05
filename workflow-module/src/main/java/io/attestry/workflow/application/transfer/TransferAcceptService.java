package io.attestry.workflow.application.transfer;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.WorkflowLedgerOutboxPort;
import io.attestry.workflow.application.port.TransferOwnershipUpdatePort;
import io.attestry.workflow.application.port.TransferProductReadPort;
import io.attestry.workflow.application.shipment.result.WorkflowLedgerEventEnvelope;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.transfer.command.AcceptTransferCommand;
import io.attestry.workflow.application.transfer.result.AcceptTransferResult;
import io.attestry.workflow.application.usecase.DelegationLifecycleUseCase;
import io.attestry.workflow.application.usecase.TransferAcceptUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.transfer.model.AcceptMethod;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferStatus;
import io.attestry.workflow.domain.transfer.model.TransferType;
import io.attestry.workflow.domain.transfer.policy.TransferAcceptPolicy;
import io.attestry.workflow.domain.transfer.policy.TransferAcceptPolicy.TransferAcceptContext;
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
    private final WorkflowLedgerOutboxPort outboxPort;
    private final DelegationLifecycleUseCase delegationLifecycleUseCase;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final TransferHashSupport hashSupport;
    private final TransferAcceptPolicy acceptPolicy;
    private final Clock clock;

    public TransferAcceptService(
        TokenTransferRepository transferRepository,
        TransferProductReadPort productReadPort,
        TransferOwnershipUpdatePort ownershipUpdatePort,
        WorkflowLedgerOutboxPort outboxPort,
        DelegationLifecycleUseCase delegationLifecycleUseCase,
        WorkflowAuthorizationSupport authorizationSupport,
        TransferHashSupport hashSupport,
        TransferAcceptPolicy acceptPolicy,
        Clock clock
    ) {
        this.transferRepository = transferRepository;
        this.productReadPort = productReadPort;
        this.ownershipUpdatePort = ownershipUpdatePort;
        this.outboxPort = outboxPort;
        this.delegationLifecycleUseCase = delegationLifecycleUseCase;
        this.authorizationSupport = authorizationSupport;
        this.hashSupport = hashSupport;
        this.acceptPolicy = acceptPolicy;
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

        // 1. 조회 + fail-fast 검증
        TokenTransfer transfer = findAndValidatePending(transferId, now);

        // 2. 도메인 정책 (패스포트 상태 + C2C 소유권)
        TransferAcceptContext context = resolveAcceptContext(transfer);
        acceptPolicy.assertAcceptable(context);

        // 3. 자격증명 검증 (부작용 포함)
        verifyCredential(transfer, command, now);

        // 4. 완료 처리
        TokenTransfer completed = completeTransfer(transfer, principal.userId(), now);

        // 5. 후처리 (소유권 + 위임 소비 + 아웃박스)
        return applyPostAcceptEffects(completed, now);
    }

    private TokenTransfer findAndValidatePending(String transferId, Instant now) {
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
        return transfer;
    }

    private TransferAcceptContext resolveAcceptContext(TokenTransfer transfer) {
        TransferProductReadPort.TransferPassportState state = productReadPort.findPassportState(transfer.passportId())
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport not found"));

        boolean isC2C = transfer.transferType() == TransferType.C2C;
        String currentOwnerId = null;
        if (isC2C) {
            currentOwnerId = productReadPort.findCurrentOwnerId(transfer.passportId())
                .orElse(null);
        }

        return new TransferAcceptContext(
            isC2C,
            state.riskFlag(),
            currentOwnerId,
            transfer.fromOwnerId()
        );
    }

    private void verifyCredential(TokenTransfer transfer, AcceptTransferCommand command, Instant now) {
        if (transfer.acceptMethod() == AcceptMethod.QR) {
            if (!transfer.verifyQrNonce(command.qrNonce())) {
                throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_NONCE_MISMATCH, "QR nonce does not match");
            }
        } else {
            TokenTransfer incremented = transfer.incrementAttempt();
            transferRepository.save(incremented);

            if (!hashSupport.verify(command.password(), incremented.codeHash(), incremented.codeSalt())) {
                if (incremented.isBruteForceBlocked()) {
                    TokenTransfer autoCancelled = incremented.cancel(null, now);
                    transferRepository.save(autoCancelled);
                }
                throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_CODE_MISMATCH, "Password does not match");
            }
        }
    }

    private TokenTransfer completeTransfer(TokenTransfer transfer, String toOwnerId, Instant now) {
        TokenTransfer completed = transfer.complete(toOwnerId, now);
        return transferRepository.save(completed);
    }

    private AcceptTransferResult applyPostAcceptEffects(TokenTransfer completed, Instant now) {
        ownershipUpdatePort.upsertOwner(completed.passportId(), completed.toOwnerId(), now);

        if (completed.transferType() == TransferType.B2C) {
            delegationLifecycleUseCase.consumeByPassportId(completed.passportId());
        }

        WorkflowLedgerEventEnvelope envelope = completed.transferType() == TransferType.B2C
            ? WorkflowLedgerEventEnvelope.ownershipClaimed(completed)
            : WorkflowLedgerEventEnvelope.ownershipTransferCompleted(completed);

        String outboxEventId = outboxPort.enqueue(envelope);

        return new AcceptTransferResult(
            completed.transferId(), completed.passportId(),
            completed.status().name(), completed.toOwnerId(),
            completed.completedAt(), outboxEventId
        );
    }
}

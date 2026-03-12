package io.attestry.workflow.application.transfer;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.transfer.policy.TransferAccessPolicy;
import io.attestry.workflow.application.transfer.result.CancelTransferResult;
import io.attestry.workflow.application.usecase.TransferCancelUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferStatus;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TransferCancelService implements TransferCancelUseCase {

    private final TokenTransferRepository transferRepository;
    private final TransferAccessPolicy accessPolicy;
    private final TransferCancelExecutor cancelExecutor;
    private final Clock clock;


    @Override
    @Transactional
    public CancelTransferResult cancel(AuthPrincipal principal, String transferId) {
        TokenTransfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.TRANSFER_NOT_FOUND, "Transfer not found"));

        if (transfer.status() != TransferStatus.PENDING) {
            throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_INVALID_STATE, "Only PENDING transfer can be cancelled");
        }

        accessPolicy.assertCancelAccess(principal, transferId, transfer);

        return cancelExecutor.cancel(transfer, principal.userId(), Instant.now(clock));
    }
}

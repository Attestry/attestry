package io.attestry.workflow.application.transfer.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.transfer.internal.TransferAccessPolicy;
import io.attestry.workflow.application.transfer.result.CancelTransferResult;
import io.attestry.workflow.application.transfer.internal.TransferCancelExecutor;
import io.attestry.workflow.application.transfer.internal.TransferLookupService;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TransferCancelService implements TransferCancelUseCase {

    private final TransferAccessPolicy accessPolicy;
    private final TransferCancelExecutor cancelExecutor;
    private final TransferLookupService transferLookupService;
    private final Clock clock;

    @Override
    @Transactional
    public CancelTransferResult cancel(WorkflowActorContext principal, String transferId) {
        TokenTransfer transfer = transferLookupService.getPendingForCancel(transferId);
        accessPolicy.assertCancelAccess(principal, transferId, transfer);
        return cancelExecutor.cancel(transfer, principal.userId(), Instant.now(clock));
    }
}

package io.attestry.workflow.application.transfer.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.transfer.internal.TransferAccessPolicy;
import io.attestry.workflow.application.transfer.result.AcceptTransferResult;
import io.attestry.workflow.application.transfer.internal.TransferAcceptExecutor;
import io.attestry.workflow.application.transfer.internal.TransferContextResolver;
import io.attestry.workflow.application.transfer.internal.TransferLookupService;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.policy.TransferAcceptPolicy.TransferAcceptContext;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferAcceptService implements TransferAcceptUseCase {

    private final TransferAccessPolicy accessPolicy;
    private final TransferContextResolver contextResolver;
    private final TransferAcceptExecutor acceptExecutor;
    private final TransferLookupService transferLookupService;
    private final Clock clock;

    @Override
    @Transactional
    public AcceptTransferResult accept(
        WorkflowActorContext principal,
        String transferId,
        AcceptTransferCommand command
    ) {
        accessPolicy.assertAcceptAccess(principal, transferId);
        TokenTransfer transfer = transferLookupService.getPendingForAccept(transferId, java.time.Instant.now(clock));
        TransferAcceptContext context = contextResolver.resolveAcceptContext(transfer);
        return acceptExecutor.accept(principal.userId(), command, context, transfer);
    }
}

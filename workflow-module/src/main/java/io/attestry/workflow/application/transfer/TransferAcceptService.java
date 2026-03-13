package io.attestry.workflow.application.transfer;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.transfer.command.AcceptTransferCommand;
import io.attestry.workflow.application.transfer.policy.TransferAccessPolicy;
import io.attestry.workflow.application.transfer.result.AcceptTransferResult;
import io.attestry.workflow.application.transfer.support.TransferContextResolver;
import io.attestry.workflow.application.usecase.TransferAcceptUseCase;
import io.attestry.workflow.domain.transfer.policy.TransferAcceptPolicy.TransferAcceptContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferAcceptService implements TransferAcceptUseCase {

    private final TransferAccessPolicy accessPolicy;
    private final TransferContextResolver contextResolver;
    private final TransferAcceptExecutor acceptExecutor;


    @Override
    @Transactional
    public AcceptTransferResult accept(
        AuthPrincipal principal,
        String transferId,
        AcceptTransferCommand command
    ) {
        accessPolicy.assertAcceptAccess(principal, transferId);
        TransferAcceptContext context = contextResolver.resolveAcceptContext(transferId);
        return acceptExecutor.accept(principal.userId(), command, context, transferId);
    }
}

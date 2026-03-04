package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.transfer.command.AcceptTransferCommand;
import io.attestry.workflow.application.transfer.result.AcceptTransferResult;

public interface TransferAcceptUseCase {

    AcceptTransferResult accept(
        AuthPrincipal principal,
        String transferId,
        AcceptTransferCommand command
    );
}

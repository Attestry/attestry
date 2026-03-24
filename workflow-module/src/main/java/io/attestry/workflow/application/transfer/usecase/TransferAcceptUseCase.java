package io.attestry.workflow.application.transfer.usecase;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.transfer.command.AcceptTransferCommand;
import io.attestry.workflow.application.transfer.result.AcceptTransferResult;

public interface TransferAcceptUseCase {

    AcceptTransferResult accept(
        WorkflowActorContext principal,
        String transferId,
        AcceptTransferCommand command
    );
}

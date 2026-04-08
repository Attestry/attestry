package io.attestry.workflow.application.transfer.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.transfer.result.CancelTransferResult;

public interface TransferCancelUseCase {

    CancelTransferResult cancel(
        WorkflowActorContext principal,
        String transferId
    );
}

package io.attestry.workflow.application.transfer.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.transfer.command.CreateB2CTransferCommand;
import io.attestry.workflow.application.transfer.command.CreateC2CTransferCommand;
import io.attestry.workflow.application.transfer.result.CreateTransferResult;
import java.util.Optional;

public interface TransferCreateUseCase {

    CreateTransferResult createC2C(
        WorkflowActorContext principal,
        String passportId,
        CreateC2CTransferCommand command
    );

    CreateTransferResult createB2C(
        WorkflowActorContext principal,
        String tenantId,
        String passportId,
        CreateB2CTransferCommand command
    );

    Optional<CreateTransferResult> findLatestActivePendingByPassportId(
        WorkflowActorContext principal,
        String passportId
    );

    Optional<CreateTransferResult> findLatestActivePendingB2CByPassportId(
        WorkflowActorContext principal,
        String tenantId,
        String passportId
    );
}

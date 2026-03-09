package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.transfer.command.CreateB2CTransferCommand;
import io.attestry.workflow.application.transfer.command.CreateC2CTransferCommand;
import io.attestry.workflow.application.transfer.result.CreateTransferResult;
import java.util.Optional;

public interface TransferCreateUseCase {

    CreateTransferResult createC2C(
        AuthPrincipal principal,
        String passportId,
        CreateC2CTransferCommand command
    );

    CreateTransferResult createB2C(
        AuthPrincipal principal,
        String tenantId,
        String passportId,
        CreateB2CTransferCommand command
    );

    Optional<CreateTransferResult> findLatestActivePendingByPassportId(
        AuthPrincipal principal,
        String passportId
    );

    Optional<CreateTransferResult> findLatestActivePendingB2CByPassportId(
        AuthPrincipal principal,
        String tenantId,
        String passportId
    );
}

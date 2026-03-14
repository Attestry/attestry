package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.transfer.result.CancelTransferResult;

public interface TransferCancelUseCase {

    CancelTransferResult cancel(
        AuthPrincipal principal,
        String transferId
    );
}

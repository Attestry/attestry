package io.attestry.workflow.application.transfer.command;

import io.attestry.workflow.domain.transfer.model.AcceptMethod;
import java.time.Instant;

public record CreateC2CTransferCommand(
    AcceptMethod acceptMethod,
    String password,
    Instant expiresAt
) {
}

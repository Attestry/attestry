package io.attestry.workflow.interfaces.transfer.dto.request;

import io.attestry.workflow.domain.transfer.model.AcceptMethod;

import java.time.Instant;

public record CreateTransferRequest(
        AcceptMethod acceptMethod,
        String password,
        Instant expiresAt
) {
}

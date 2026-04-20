package io.attestry.workflow.interfaces.transfer.dto.request;

import io.attestry.workflow.domain.transfer.model.AcceptMethod;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateTransferRequest(
        @NotNull(message = "Accept method is required")
        AcceptMethod acceptMethod,
        String password,
        @NotNull(message = "Expiration date is required")
        Instant expiresAt
) {
}

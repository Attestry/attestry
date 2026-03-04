package io.attestry.workflow.application.transfer.result;

import java.time.Instant;

public record CreateTransferResult(
    String transferId,
    String passportId,
    String transferType,
    String status,
    String acceptMethod,
    String qrNonce,
    Instant expiresAt
) {
}

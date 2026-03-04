package io.attestry.workflow.application.transfer.result;

import java.time.Instant;

public record CancelTransferResult(
    String transferId,
    String passportId,
    String status,
    Instant cancelledAt
) {
}

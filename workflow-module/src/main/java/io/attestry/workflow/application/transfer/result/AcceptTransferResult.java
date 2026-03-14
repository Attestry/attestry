package io.attestry.workflow.application.transfer.result;

import java.time.Instant;

public record AcceptTransferResult(
    String transferId,
    String passportId,
    String status,
    String toOwnerId,
    Instant completedAt,
    String outboxEventId
) {
}

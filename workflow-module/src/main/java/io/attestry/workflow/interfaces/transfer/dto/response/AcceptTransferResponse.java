package io.attestry.workflow.interfaces.transfer.dto.response;

import io.attestry.workflow.application.transfer.result.AcceptTransferResult;

import java.time.Instant;

public record AcceptTransferResponse(
        String passportId,
        String status,
        String toOwnerId,
        Instant completedAt,
        String outboxEventId
) {
    public static AcceptTransferResponse from(AcceptTransferResult result) {
        return new AcceptTransferResponse(
                result.passportId(),
                result.status(), result.toOwnerId(),
                result.completedAt(), result.outboxEventId()
        );
    }
}

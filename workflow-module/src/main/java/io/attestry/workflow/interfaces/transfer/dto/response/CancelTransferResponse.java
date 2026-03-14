package io.attestry.workflow.interfaces.transfer.dto.response;

import io.attestry.workflow.application.transfer.result.CancelTransferResult;

import java.time.Instant;

public record CancelTransferResponse(
        String transferId,
        String passportId,
        String status,
        Instant cancelledAt
) {
    public static CancelTransferResponse from(CancelTransferResult result) {
        return new CancelTransferResponse(
                result.transferId(), result.passportId(),
                result.status(), result.cancelledAt()
        );
    }
}
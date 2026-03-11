package io.attestry.workflow.interfaces.transfer.dto.response;

import io.attestry.workflow.application.transfer.result.CreateTransferResult;

import java.time.Instant;

public record CreateTransferResponse(
        String transferId,
        String passportId,
        String transferType,
        String status,
        String acceptMethod,
        String qrNonce,
        Instant expiresAt
) {
    public static CreateTransferResponse from(CreateTransferResult result) {
        return new CreateTransferResponse(
                result.transferId(), result.passportId(),
                result.transferType(), result.status(),
                result.acceptMethod(), result.qrNonce(), result.expiresAt()
        );
    }
}

package io.attestry.product.interfaces.http.command.dto.response;

import io.attestry.product.application.dto.result.RetireResult;

public record RetireResponse(
    String assetId,
    String assetState,
    String outboxEventId
) {
    public static RetireResponse from(RetireResult result) {
        return new RetireResponse(result.assetId(), result.assetState(), result.outboxEventId());
    }
}

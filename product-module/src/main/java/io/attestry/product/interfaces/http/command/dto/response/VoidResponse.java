package io.attestry.product.interfaces.http.command.dto.response;

import io.attestry.product.application.command.result.VoidResult;

public record VoidResponse(
    String assetId,
    String assetState,
    String outboxEventId
) {
    public static VoidResponse from(VoidResult result) {
        return new VoidResponse(result.assetId(), result.assetState(), result.outboxEventId());
    }
}

package io.attestry.product.application.dto.result;

public record RetireResult(
    String assetId,
    String assetState,
    String outboxEventId
) {
}

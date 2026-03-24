package io.attestry.product.application.command.result;

public record RetireResult(
    String assetId,
    String assetState,
    String outboxEventId
) {
}

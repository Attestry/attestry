package io.attestry.product.application.command.result;

public record VoidResult(String assetId, String assetState, String outboxEventId) {
}

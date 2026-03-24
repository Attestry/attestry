package io.attestry.product.application.command.result;

public record MintedProductResult(
    String assetId,
    String passportId,
    String qrPublicCode,
    String outboxEventId,
    String ledgerEventCategory,
    String ledgerEventAction
) {
}

package io.attestry.product.application.dto.result;

public record MintedProductResult(
    String assetId,
    String passportId,
    String qrPublicCode,
    String outboxEventId,
    String ledgerEventCategory,
    String ledgerEventAction
) {
}

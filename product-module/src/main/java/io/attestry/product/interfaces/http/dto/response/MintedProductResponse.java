package io.attestry.product.interfaces.http.dto.response;

import io.attestry.product.application.dto.result.MintedProductResult;

public record MintedProductResponse(
    String assetId,
    String passportId,
    String qrPublicCode,
    String outboxEventId,
    String ledgerEventCategory,
    String ledgerEventAction
) {
    public static MintedProductResponse from(MintedProductResult result) {
        return new MintedProductResponse(
            result.assetId(),
            result.passportId(),
            result.qrPublicCode(),
            result.outboxEventId(),
            result.ledgerEventCategory(),
            result.ledgerEventAction()
        );
    }
}

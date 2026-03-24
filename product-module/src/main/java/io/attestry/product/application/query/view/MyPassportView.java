package io.attestry.product.application.query.view;

import java.time.Instant;

public record MyPassportView(
    String passportId,
    String qrPublicCode,
    String tenantId,
    String assetId,
    String serialNumber,
    String modelName,
    String assetState,
    String riskFlag,
    Instant ownedSince
) {
}

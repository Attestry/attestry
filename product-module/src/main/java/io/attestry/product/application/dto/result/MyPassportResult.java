package io.attestry.product.application.dto.result;

import java.time.Instant;

public record MyPassportResult(
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

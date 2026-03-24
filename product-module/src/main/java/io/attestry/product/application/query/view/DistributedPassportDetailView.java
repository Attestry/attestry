package io.attestry.product.application.query.view;

import java.time.Instant;

public record DistributedPassportDetailView(
    String passportId,
    String qrPublicCode,
    String serialNumber,
    String modelId,
    String modelName,
    String assetState,
    String riskFlag,
    Instant manufacturedAt,
    String productionBatch,
    String factoryCode
) {
}

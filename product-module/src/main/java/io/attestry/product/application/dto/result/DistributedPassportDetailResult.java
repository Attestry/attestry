package io.attestry.product.application.dto.result;

import java.time.Instant;

public record DistributedPassportDetailResult(
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

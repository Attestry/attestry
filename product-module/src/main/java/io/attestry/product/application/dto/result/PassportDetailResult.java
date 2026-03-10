package io.attestry.product.application.dto.result;

import java.time.Instant;

public record PassportDetailResult(
    String passportId,
    String qrPublicCode,
    String tenantId,
    String assetId,
    String serialNumber,
    String modelId,
    String modelName,
    Instant manufacturedAt,
    String productionBatch,
    String factoryCode,
    String assetState,
    String riskFlag,
    Instant createdAt,
    String publicUrl,
    ShipmentDetailResult shipment,
    DistributionDetailResult distribution
) {
}

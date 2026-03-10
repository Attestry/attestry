package io.attestry.product.application.dto.result;

import java.time.Instant;

public record DistributedPassportResult(
    String passportId,
    String qrPublicCode,
    String assetId,
    String serialNumber,
    String modelId,
    String modelName,
    String assetState,
    String riskFlag,
    String permissionId,
    Instant expiresAt,
    String sourceTenantId,
    String targetTenantId,
    String permissionStatus,
    Instant distributedAt
) {
}

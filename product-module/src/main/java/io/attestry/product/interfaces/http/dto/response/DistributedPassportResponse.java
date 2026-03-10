package io.attestry.product.interfaces.http.dto.response;

import io.attestry.product.application.dto.result.DistributedPassportResult;
import java.time.Instant;

public record DistributedPassportResponse(
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
    public static DistributedPassportResponse from(DistributedPassportResult result) {
        return new DistributedPassportResponse(
            result.passportId(), result.qrPublicCode(), result.assetId(),
            result.serialNumber(), result.modelId(), result.modelName(),
            result.assetState(), result.riskFlag(), result.permissionId(),
            result.expiresAt(), result.sourceTenantId(), result.targetTenantId(),
            result.permissionStatus(), result.distributedAt()
        );
    }
}

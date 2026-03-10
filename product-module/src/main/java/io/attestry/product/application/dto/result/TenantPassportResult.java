package io.attestry.product.application.dto.result;

import java.time.Instant;

public record TenantPassportResult(
    String passportId,
    String serialNumber,
    String modelId,
    String modelName,
    String assetState,
    Instant createdAt
) {
}

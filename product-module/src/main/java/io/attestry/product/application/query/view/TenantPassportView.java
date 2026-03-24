package io.attestry.product.application.query.view;

import java.time.Instant;

public record TenantPassportView(
    String passportId,
    String serialNumber,
    String modelId,
    String modelName,
    String assetState,
    Instant createdAt
) {
}

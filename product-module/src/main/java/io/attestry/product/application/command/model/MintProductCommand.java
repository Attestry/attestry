package io.attestry.product.application.command.model;

import java.time.Instant;

public record MintProductCommand(
    String tenantId,
    String serialNumber,
    String modelId,
    String modelName,
    Instant manufacturedAt,
    String productionBatch,
    String factoryCode,
    String componentRootHash
) {
}

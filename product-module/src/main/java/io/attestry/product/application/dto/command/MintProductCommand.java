package io.attestry.product.application.dto.command;

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

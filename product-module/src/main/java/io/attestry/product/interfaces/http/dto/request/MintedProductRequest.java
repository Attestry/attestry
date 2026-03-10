package io.attestry.product.interfaces.http.dto.request;

import java.time.Instant;

//TODO("valid check")
public record MintedProductRequest(
    String serialNumber,
    String modelId,
    String modelName,

    Instant manufacturedAt,
    String productionBatch,
    String factoryCode,
    String componentRootHash
) {
}

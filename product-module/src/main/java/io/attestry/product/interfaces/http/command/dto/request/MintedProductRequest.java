package io.attestry.product.interfaces.http.command.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record MintedProductRequest(
    @NotBlank(message = "Serial number is required")
    String serialNumber,

    @NotBlank(message = "Model ID is required")
    String modelId,

    @NotBlank(message = "Model name is required")
    String modelName,

    @NotNull(message = "Manufactured date is required")
    Instant manufacturedAt,

    @NotBlank(message = "Production batch is required")
    String productionBatch,

    @NotBlank(message = "Factory code is required")
    String factoryCode,

    String componentRootHash
) {
}

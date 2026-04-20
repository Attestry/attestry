package io.attestry.workflow.interfaces.claim.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ApproveClaimRequest(
    @NotNull(message = "Manufactured date is required")
    Instant manufacturedAt,
    @NotBlank(message = "Production batch is required")
    String productionBatch,
    @NotBlank(message = "Factory code is required")
    String factoryCode
) {
}

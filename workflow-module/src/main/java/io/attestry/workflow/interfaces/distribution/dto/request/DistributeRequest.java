package io.attestry.workflow.interfaces.distribution.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;

public record DistributeRequest(
    @NotEmpty(message = "Passport IDs are required")
    List<String> passportIds,
    Instant expiresAt,
    String note
) {
}

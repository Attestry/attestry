package io.attestry.product.interfaces.http.command.dto.request;

import io.attestry.product.domain.permission.model.PermissionScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record GrantPermissionRequest(
    @NotBlank(message = "Seller tenant ID is required")
    String sellerTenantId,

    @NotNull(message = "Permission scope is required")
    PermissionScope scope,

    Instant expiresAt
) {
}

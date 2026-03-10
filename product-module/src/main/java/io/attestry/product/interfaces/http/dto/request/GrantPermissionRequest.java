package io.attestry.product.interfaces.http.dto.request;

import io.attestry.product.domain.permission.model.PermissionScope;
import java.time.Instant;

public record GrantPermissionRequest(
    String sellerTenantId,
    PermissionScope scope,
    Instant expiresAt
) {
}

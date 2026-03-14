package io.attestry.product.application.dto.command;

import io.attestry.product.domain.permission.model.PermissionScope;
import java.time.Instant;

public record GrantCommand(String passportId, String sellerTenantId, PermissionScope scope, Instant expiresAt) {
}

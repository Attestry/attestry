package io.attestry.product.application.command.model;

import io.attestry.product.domain.permission.model.PermissionScope;
import java.time.Instant;

public record GrantCommand(String passportId, String sellerTenantId, PermissionScope scope, Instant expiresAt) {
}

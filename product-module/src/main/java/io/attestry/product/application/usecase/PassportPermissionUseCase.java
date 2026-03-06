package io.attestry.product.application.usecase;

import io.attestry.product.domain.permission.model.PermissionScope;
import io.attestry.userauth.application.dto.command.ActorContext;
import java.time.Instant;

public interface PassportPermissionUseCase {

    GrantResult grantPermission(ActorContext actor, GrantCommand command);

    void revokePermission(ActorContext actor, String permissionId);

    void suspendPermission(ActorContext actor, String permissionId);

    record GrantCommand(String passportId, String sellerTenantId, PermissionScope scope, Instant expiresAt) {
    }

    record GrantResult(String permissionId, String passportId, String sellerTenantId, String scope) {
    }
}

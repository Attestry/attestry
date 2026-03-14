package io.attestry.product.application.usecase;

import io.attestry.product.application.dto.command.GrantCommand;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.dto.result.GrantResult;

public interface PassportPermissionUseCase {

    GrantResult grantPermission(ProductActor actor, GrantCommand command);

    void revokePermission(ProductActor actor, String permissionId);

    void suspendPermission(ProductActor actor, String permissionId);
}

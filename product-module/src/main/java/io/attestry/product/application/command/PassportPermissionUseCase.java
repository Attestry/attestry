package io.attestry.product.application.command;

import io.attestry.product.application.command.model.GrantCommand;
import io.attestry.product.application.command.result.GrantResult;
import io.attestry.product.application.common.ProductActor;

public interface PassportPermissionUseCase {

    GrantResult grantPermission(ProductActor actor, GrantCommand command);

    void revokePermission(ProductActor actor, String permissionId);

    void suspendPermission(ProductActor actor, String permissionId);
}

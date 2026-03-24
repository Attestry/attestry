package io.attestry.product.interfaces.http.command.dto.response;

import io.attestry.product.application.command.result.GrantResult;

public record GrantPermissionResponse(
    String permissionId,
    String passportId,
    String sellerTenantId,
    String scope
) {
    public static GrantPermissionResponse from(GrantResult result) {
        return new GrantPermissionResponse(
            result.permissionId(), result.passportId(), result.sellerTenantId(), result.scope()
        );
    }
}

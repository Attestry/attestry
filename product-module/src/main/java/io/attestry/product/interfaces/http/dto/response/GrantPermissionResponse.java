package io.attestry.product.interfaces.http.dto.response;

import io.attestry.product.application.dto.result.GrantResult;

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

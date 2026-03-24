package io.attestry.product.application.command.result;

public record GrantResult(String permissionId, String passportId, String sellerTenantId, String scope) {
}

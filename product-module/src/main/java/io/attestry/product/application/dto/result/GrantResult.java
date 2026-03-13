package io.attestry.product.application.dto.result;

public record GrantResult(String permissionId, String passportId, String sellerTenantId, String scope) {
}

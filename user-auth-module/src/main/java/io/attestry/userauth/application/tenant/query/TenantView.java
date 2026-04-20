package io.attestry.userauth.application.tenant.query;

public record TenantView(
        String tenantId,
        String name,
        String region,
        String address,
        String type,
        String status) {
}

package io.attestry.userauth.application.tenant.view;

public record TenantView(
        String tenantId,
        String name,
        String region,
        String address,
        String type,
        String status) {
}

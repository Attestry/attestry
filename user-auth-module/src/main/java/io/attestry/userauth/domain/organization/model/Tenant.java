package io.attestry.userauth.domain.organization.model;

public record Tenant(
    String tenantId,
    String name,
    String region,
    TenantStatus status
) {
    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }
}

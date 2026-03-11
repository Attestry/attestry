package io.attestry.workflow.application.port.common;

public interface TenantReadPort {
    boolean existsActiveTenant(String tenantId);

    String findTenantName(String tenantId);

    String findTenantType(String tenantId);

    TenantSummary findTenantSummary(String tenantId);

    record TenantSummary(
        String tenantId,
        String name,
        String region,
        String address,
        String type
    ) {
    }
}

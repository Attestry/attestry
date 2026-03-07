package io.attestry.workflow.application.port;

import java.util.List;

public interface TenantReadPort {
    boolean existsActiveTenant(String tenantId);

    String findTenantName(String tenantId);

    List<TenantSummary> searchActiveTenantsByName(String name);

    record TenantSummary(
        String tenantId,
        String name,
        String region,
        String type
    ) {
    }
}

package io.attestry.workflow.application.port.common;

import java.util.List;
import java.util.Map;

public interface TenantReadPort {
    boolean existsActiveTenant(String tenantId);

    TenantSummary findTenantSummary(String tenantId);

    Map<String, String> findTenantNamesByIds(List<String> tenantIds);

    Map<String, TenantSummary> findTenantSummariesByIds(List<String> tenantIds);

    record TenantSummary(
        String tenantId,
        String name,
        String region,
        String address,
        String type
    ) {
    }
}

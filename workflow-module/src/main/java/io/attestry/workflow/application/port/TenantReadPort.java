package io.attestry.workflow.application.port;

import java.util.List;

public interface TenantReadPort {
    boolean existsActiveTenant(String tenantId);

    String findTenantName(String tenantId);

    String findTenantType(String tenantId);

    TenantSummary findTenantSummary(String tenantId);

    List<TenantSummary> searchActiveTenantsByName(String name);

    PagedTenantSummary searchActiveTenantsByTypeAndName(String type, String name, int page, int size);

    record TenantSummary(
        String tenantId,
        String name,
        String region,
        String type
    ) {
    }

    record PagedTenantSummary(
        List<TenantSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }
}

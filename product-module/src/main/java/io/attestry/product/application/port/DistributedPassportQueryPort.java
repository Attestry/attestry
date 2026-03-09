package io.attestry.product.application.port;

import java.time.Instant;
import java.util.List;

public interface DistributedPassportQueryPort {

    PagedResult findByTargetTenant(
        String tenantId,
        int page,
        int size,
        String keyword,
        String sourceTenantId
    );

    record DistributedPassportView(
        String passportId,
        String qrPublicCode,
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        String assetState,
        String riskFlag,
        String permissionId,
        Instant expiresAt,
        String sourceTenantId,
        String targetTenantId,
        String permissionStatus,
        Instant distributedAt
    ) {
    }

    record PagedResult(List<DistributedPassportView> content, int page, int size, long totalElements, int totalPages) {
    }
}

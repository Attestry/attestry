package io.attestry.product.application.port.projection;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ProductRetailAccessProjectionPort {

    PagedRetailAccessResult findAccessiblePassports(
        String tenantId,
        int page,
        int size,
        String keyword,
        String sourceTenantId
    );

    Optional<RetailAccessDetailView> findAccessiblePassportDetail(String tenantId, String passportId);

    record RetailAccessRow(
        String passportId,
        String qrPublicCode,
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        String assetState,
        String riskFlag,
        String accessSourceType,
        String accessSourceId,
        Instant expiresAt,
        String sourceTenantId,
        String targetTenantId,
        String accessStatus,
        Instant grantedAt
    ) {
    }

    record RetailAccessDetailView(
        String passportId,
        String qrPublicCode,
        String serialNumber,
        String modelId,
        String modelName,
        String assetState,
        String riskFlag,
        Instant manufacturedAt,
        String productionBatch,
        String factoryCode,
        String accessSourceType,
        String accessSourceId,
        Instant updatedAt
    ) {
    }

    record PagedRetailAccessResult(
        List<RetailAccessRow> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }
}

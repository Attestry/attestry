package io.attestry.product.domain.passport.model;

import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import java.time.Instant;

public record MintProductInput(
    String tenantId,
    String groupId,
    String serialNumber,
    String modelId,
    String modelName,
    Instant manufacturedAt,
    String productionBatch,
    String factoryCode,
    String componentRootHash
) {

    public static MintProductInput of(
        String tenantId,
        String groupId,
        String serialNumber,
        String modelId,
        String modelName,
        Instant manufacturedAt,
        String productionBatch,
        String factoryCode,
        String componentRootHash
    ) {
        if (manufacturedAt == null) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, "manufacturedAt is required");
        }
        String normalizedComponentHash = normalizeBlank(componentRootHash);
        if (normalizedComponentHash != null && normalizedComponentHash.length() != 64) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, "componentRootHash must be 64 chars");
        }
        return new MintProductInput(
            requireText(tenantId, "tenantId"),
            requireText(groupId, "groupId"),
            requireText(serialNumber, "serialNumber"),
            normalizeBlank(modelId),
            requireText(modelName, "modelName"),
            manufacturedAt,
            normalizeBlank(productionBatch),
            normalizeBlank(factoryCode),
            normalizedComponentHash
        );
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeBlank(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}

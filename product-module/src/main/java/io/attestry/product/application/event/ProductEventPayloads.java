package io.attestry.product.application.event;

import io.attestry.product.domain.passport.model.ProductAsset;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.domain.passport.model.RiskFlag;
import java.util.HashMap;
import java.util.Map;

public final class ProductEventPayloads {

    private ProductEventPayloads() {
    }

    public record MintedPayload(
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        String manufacturedAt,
        String qrPublicCode,
        String productionBatch,
        String factoryCode,
        String componentRootHash,
        String tenantId,
        String assetState,
        String riskFlagProjection
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("assetId", assetId);
            map.put("serialNumber", serialNumber);
            map.put("modelId", modelId);
            map.put("modelName", modelName);
            map.put("manufacturedAt", manufacturedAt);
            map.put("qrPublicCode", qrPublicCode);
            map.put("productionBatch", productionBatch);
            map.put("factoryCode", factoryCode);
            map.put("componentRootHash", componentRootHash);
            map.put("tenantId", tenantId);
            map.put("assetState", assetState);
            map.put("riskFlagProjection", riskFlagProjection);
            return Map.copyOf(map);
        }
    }

    public record VoidedPayload(
        String assetId,
        String reason,
        String note,
        String tenantId,
        String assetState,
        String riskFlagProjection,
        String serialNumber,
        String modelId,
        String modelName,
        String productionBatch,
        String factoryCode,
        String manufacturedAt
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("assetId", assetId);
            map.put("reason", reason);
            map.put("note", note);
            putProjectionFields(map);
            return Map.copyOf(map);
        }

        private void putProjectionFields(Map<String, Object> map) {
            map.put("tenantId", tenantId);
            map.put("assetState", assetState);
            map.put("riskFlagProjection", riskFlagProjection);
            map.put("serialNumber", serialNumber);
            map.put("modelId", modelId);
            map.put("modelName", modelName);
            map.put("productionBatch", productionBatch);
            map.put("factoryCode", factoryCode);
            map.put("manufacturedAt", manufacturedAt);
        }
    }

    public record RetiredPayload(
        String assetId,
        String tenantId,
        String assetState,
        String riskFlagProjection,
        String serialNumber,
        String modelId,
        String modelName,
        String productionBatch,
        String factoryCode,
        String manufacturedAt
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("assetId", assetId);
            map.put("tenantId", tenantId);
            map.put("assetState", assetState);
            map.put("riskFlagProjection", riskFlagProjection);
            map.put("serialNumber", serialNumber);
            map.put("modelId", modelId);
            map.put("modelName", modelName);
            map.put("productionBatch", productionBatch);
            map.put("factoryCode", factoryCode);
            map.put("manufacturedAt", manufacturedAt);
            return Map.copyOf(map);
        }
    }

    public record RiskFlaggedPayload(
        String assetId,
        String riskFlag,
        String reportedBy,
        String policeReportNo,
        String tenantId,
        String assetState,
        String riskFlagProjection,
        String serialNumber,
        String modelId,
        String modelName,
        String productionBatch,
        String factoryCode,
        String manufacturedAt
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("assetId", assetId);
            map.put("riskFlag", riskFlag);
            map.put("reportedBy", reportedBy);
            if (policeReportNo != null) {
                map.put("policeReportNo", policeReportNo);
            }
            map.put("tenantId", tenantId);
            map.put("assetState", assetState);
            map.put("riskFlagProjection", riskFlagProjection);
            map.put("serialNumber", serialNumber);
            map.put("modelId", modelId);
            map.put("modelName", modelName);
            map.put("productionBatch", productionBatch);
            map.put("factoryCode", factoryCode);
            map.put("manufacturedAt", manufacturedAt);
            return Map.copyOf(map);
        }
    }

    public record RiskClearedPayload(
        String assetId,
        String clearedRiskFlag,
        String tenantId,
        String assetState,
        String riskFlagProjection,
        String serialNumber,
        String modelId,
        String modelName,
        String productionBatch,
        String factoryCode,
        String manufacturedAt
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("assetId", assetId);
            map.put("clearedRiskFlag", clearedRiskFlag);
            map.put("tenantId", tenantId);
            map.put("assetState", assetState);
            map.put("riskFlagProjection", riskFlagProjection);
            map.put("serialNumber", serialNumber);
            map.put("modelId", modelId);
            map.put("modelName", modelName);
            map.put("productionBatch", productionBatch);
            map.put("factoryCode", factoryCode);
            map.put("manufacturedAt", manufacturedAt);
            return Map.copyOf(map);
        }
    }

    public static MintedPayload mintedPayload(ProductPassport passport) {
        ProductAsset asset = passport.getAsset();
        return new MintedPayload(
            asset.getAssetId(),
            asset.getSerialNumber(),
            nullToEmpty(asset.getModelId()),
            asset.getModelName(),
            asset.getManufacturedAt().toString(),
            passport.getQrPublicCode(),
            nullToEmpty(asset.getProductionBatch()),
            nullToEmpty(asset.getFactoryCode()),
            nullToEmpty(asset.getComponentRootHash()),
            passport.getTenantId(),
            asset.getAssetState().name(),
            asset.getRiskFlag() == RiskFlag.NONE ? "NONE" : "FLAGGED"
        );
    }

    public static VoidedPayload voidedPayload(ProductPassport passport) {
        ProductAsset asset = passport.getAsset();
        return new VoidedPayload(
            asset.getAssetId(),
            asset.getVoidedReason().name(),
            nullToEmpty(asset.getVoidedNote()),
            passport.getTenantId(),
            asset.getAssetState().name(),
            asset.getRiskFlag() == RiskFlag.NONE ? "NONE" : "FLAGGED",
            asset.getSerialNumber(),
            nullToEmpty(asset.getModelId()),
            asset.getModelName(),
            nullToEmpty(asset.getProductionBatch()),
            nullToEmpty(asset.getFactoryCode()),
            asset.getManufacturedAt().toString()
        );
    }

    public static RetiredPayload retiredPayload(ProductPassport passport) {
        ProductAsset asset = passport.getAsset();
        return new RetiredPayload(
            asset.getAssetId(),
            passport.getTenantId(),
            asset.getAssetState().name(),
            asset.getRiskFlag() == RiskFlag.NONE ? "NONE" : "FLAGGED",
            asset.getSerialNumber(),
            nullToEmpty(asset.getModelId()),
            asset.getModelName(),
            nullToEmpty(asset.getProductionBatch()),
            nullToEmpty(asset.getFactoryCode()),
            asset.getManufacturedAt().toString()
        );
    }

    public static RiskFlaggedPayload riskFlaggedPayload(ProductPassport passport) {
        ProductAsset asset = passport.getAsset();
        return new RiskFlaggedPayload(
            asset.getAssetId(),
            asset.getRiskFlag().name(),
            nullToEmpty(asset.getRiskReportedBy()),
            asset.getPoliceReportNo(),
            passport.getTenantId(),
            asset.getAssetState().name(),
            asset.getRiskFlag() == RiskFlag.NONE ? "NONE" : "FLAGGED",
            asset.getSerialNumber(),
            nullToEmpty(asset.getModelId()),
            asset.getModelName(),
            nullToEmpty(asset.getProductionBatch()),
            nullToEmpty(asset.getFactoryCode()),
            asset.getManufacturedAt().toString()
        );
    }

    public static RiskClearedPayload riskClearedPayload(ProductPassport passport, RiskFlag clearedRiskFlag) {
        ProductAsset asset = passport.getAsset();
        return new RiskClearedPayload(
            asset.getAssetId(),
            clearedRiskFlag == null ? "" : clearedRiskFlag.name(),
            passport.getTenantId(),
            asset.getAssetState().name(),
            asset.getRiskFlag() == RiskFlag.NONE ? "NONE" : "FLAGGED",
            asset.getSerialNumber(),
            nullToEmpty(asset.getModelId()),
            asset.getModelName(),
            nullToEmpty(asset.getProductionBatch()),
            nullToEmpty(asset.getFactoryCode()),
            asset.getManufacturedAt().toString()
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

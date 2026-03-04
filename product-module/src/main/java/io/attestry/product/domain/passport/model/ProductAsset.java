package io.attestry.product.domain.passport.model;

import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import java.time.Instant;

public class ProductAsset {

    // === Immutable ===
    private final String assetId;
    private final String serialNumber;
    private final String modelId;
    private final String modelName;
    private final Instant manufacturedAt;
    private final String productionBatch;
    private final String factoryCode;
    private final String componentRootHash;
    private final Instant createdAt;

    // === Mutable ===
    private AssetState assetState;
    private RiskFlag riskFlag;
    private Instant voidedAt;
    private VoidReason voidedReason;
    private String voidedNote;
    private Instant stolenAt;
    private Instant lostAt;
    private String riskReportedBy;
    private String policeReportNo;

    private ProductAsset(
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        Instant manufacturedAt,
        String productionBatch,
        String factoryCode,
        String componentRootHash,
        AssetState assetState,
        RiskFlag riskFlag,
        Instant createdAt,
        Instant voidedAt,
        VoidReason voidedReason,
        String voidedNote,
        Instant stolenAt,
        Instant lostAt,
        String riskReportedBy,
        String policeReportNo
    ) {
        this.assetId = assetId;
        this.serialNumber = serialNumber;
        this.modelId = modelId;
        this.modelName = modelName;
        this.manufacturedAt = manufacturedAt;
        this.productionBatch = productionBatch;
        this.factoryCode = factoryCode;
        this.componentRootHash = componentRootHash;
        this.assetState = assetState;
        this.riskFlag = riskFlag;
        this.createdAt = createdAt;
        this.voidedAt = voidedAt;
        this.voidedReason = voidedReason;
        this.voidedNote = voidedNote;
        this.stolenAt = stolenAt;
        this.lostAt = lostAt;
        this.riskReportedBy = riskReportedBy;
        this.policeReportNo = policeReportNo;
    }

    // --- Factory ---

    public static ProductAsset create(String assetId, MintProductInput input, Instant now) {
        return new ProductAsset(
            requireText(assetId, "assetId"),
            input.serialNumber(),
            input.modelId(),
            input.modelName(),
            input.manufacturedAt(),
            input.productionBatch(),
            input.factoryCode(),
            input.componentRootHash(),
            AssetState.ACTIVE,
            RiskFlag.NONE,
            now,
            null, null, null,
            null, null, null, null
        );
    }

    public static ProductAsset reconstitute(
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        Instant manufacturedAt,
        String productionBatch,
        String factoryCode,
        String componentRootHash,
        AssetState assetState,
        RiskFlag riskFlag,
        Instant createdAt,
        Instant voidedAt,
        VoidReason voidedReason,
        String voidedNote,
        Instant stolenAt,
        Instant lostAt,
        String riskReportedBy,
        String policeReportNo
    ) {
        return new ProductAsset(
            assetId, serialNumber, modelId, modelName,
            manufacturedAt, productionBatch, factoryCode, componentRootHash,
            assetState, riskFlag, createdAt,
            voidedAt, voidedReason, voidedNote,
            stolenAt, lostAt, riskReportedBy, policeReportNo
        );
    }

    // --- Behaviors ---

    public void voidAsset(VoidReason reason, String note, Instant now) {
        assertActive();
        this.assetState = AssetState.VOIDED;
        this.voidedAt = now;
        this.voidedReason = reason;
        this.voidedNote = note;
        this.riskFlag = RiskFlag.NONE;
    }

    public void flagStolen(String reportedBy, String policeReportNo, Instant now) {
        assertActive();
        assertNoRisk();
        this.riskFlag = RiskFlag.STOLEN;
        this.stolenAt = now;
        this.riskReportedBy = reportedBy;
        this.policeReportNo = policeReportNo;
    }

    public void flagLost(String reportedBy, Instant now) {
        assertActive();
        assertNoRisk();
        this.riskFlag = RiskFlag.LOST;
        this.lostAt = now;
        this.riskReportedBy = reportedBy;
    }

    public void clearRisk() {
        this.riskFlag = RiskFlag.NONE;
        this.stolenAt = null;
        this.lostAt = null;
        this.riskReportedBy = null;
        this.policeReportNo = null;
    }

    // --- Assertions ---

    private void assertActive() {
        if (assetState != AssetState.ACTIVE) {
            throw new ProductDomainException(ProductErrorCode.ASSET_ALREADY_VOIDED,
                "Asset is already voided: " + assetId);
        }
    }

    private void assertNoRisk() {
        if (riskFlag != RiskFlag.NONE) {
            throw new ProductDomainException(ProductErrorCode.RISK_FLAG_ALREADY_SET,
                "Risk flag already set: " + riskFlag);
        }
    }

    // --- Getters ---

    public String getAssetId() { return assetId; }
    public String getSerialNumber() { return serialNumber; }
    public String getModelId() { return modelId; }
    public String getModelName() { return modelName; }
    public Instant getManufacturedAt() { return manufacturedAt; }
    public String getProductionBatch() { return productionBatch; }
    public String getFactoryCode() { return factoryCode; }
    public String getComponentRootHash() { return componentRootHash; }
    public AssetState getAssetState() { return assetState; }
    public RiskFlag getRiskFlag() { return riskFlag; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getVoidedAt() { return voidedAt; }
    public VoidReason getVoidedReason() { return voidedReason; }
    public String getVoidedNote() { return voidedNote; }
    public Instant getStolenAt() { return stolenAt; }
    public Instant getLostAt() { return lostAt; }
    public String getRiskReportedBy() { return riskReportedBy; }
    public String getPoliceReportNo() { return policeReportNo; }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }
}

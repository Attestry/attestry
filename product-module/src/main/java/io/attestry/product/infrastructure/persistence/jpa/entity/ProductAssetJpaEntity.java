package io.attestry.product.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "product_assets")
public class ProductAssetJpaEntity {

    @Id
    @Column(name = "asset_id", nullable = false, length = 36)
    private String assetId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "group_id", nullable = false, length = 36)
    private String groupId;

    @Column(name = "serial_number", nullable = false, length = 120)
    private String serialNumber;

    @Column(name = "model_id", length = 120)
    private String modelId;

    @Column(name = "model_name", nullable = false, length = 255)
    private String modelName;

    @Column(name = "manufactured_at", nullable = false)
    private Instant manufacturedAt;

    @Column(name = "production_batch", length = 120)
    private String productionBatch;

    @Column(name = "factory_code", length = 120)
    private String factoryCode;

    @Column(name = "component_root_hash", length = 64)
    private String componentRootHash;

    @Column(name = "asset_state", nullable = false, length = 30)
    private String assetState;

    @Column(name = "risk_flag", nullable = false, length = 30)
    private String riskFlag;

    @Column(name = "ownership_user_id", length = 36)
    private String ownershipUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "voided_reason", length = 50)
    private String voidedReason;

    @Column(name = "voided_note", columnDefinition = "TEXT")
    private String voidedNote;

    @Column(name = "stolen_at")
    private Instant stolenAt;

    @Column(name = "lost_at")
    private Instant lostAt;

    @Column(name = "risk_reported_by", length = 36)
    private String riskReportedBy;

    @Column(name = "police_report_no", length = 100)
    private String policeReportNo;

    protected ProductAssetJpaEntity() {
    }

    public ProductAssetJpaEntity(
        String assetId,
        String tenantId,
        String groupId,
        String serialNumber,
        String modelId,
        String modelName,
        Instant manufacturedAt,
        String productionBatch,
        String factoryCode,
        String componentRootHash,
        String assetState,
        String riskFlag,
        String ownershipUserId,
        Instant createdAt,
        Instant voidedAt,
        String voidedReason,
        String voidedNote,
        Instant stolenAt,
        Instant lostAt,
        String riskReportedBy,
        String policeReportNo
    ) {
        this.assetId = assetId;
        this.tenantId = tenantId;
        this.groupId = groupId;
        this.serialNumber = serialNumber;
        this.modelId = modelId;
        this.modelName = modelName;
        this.manufacturedAt = manufacturedAt;
        this.productionBatch = productionBatch;
        this.factoryCode = factoryCode;
        this.componentRootHash = componentRootHash;
        this.assetState = assetState;
        this.riskFlag = riskFlag;
        this.ownershipUserId = ownershipUserId;
        this.createdAt = createdAt;
        this.voidedAt = voidedAt;
        this.voidedReason = voidedReason;
        this.voidedNote = voidedNote;
        this.stolenAt = stolenAt;
        this.lostAt = lostAt;
        this.riskReportedBy = riskReportedBy;
        this.policeReportNo = policeReportNo;
    }

    public String getAssetId() { return assetId; }
    public String getTenantId() { return tenantId; }
    public String getGroupId() { return groupId; }
    public String getSerialNumber() { return serialNumber; }
    public String getModelId() { return modelId; }
    public String getModelName() { return modelName; }
    public Instant getManufacturedAt() { return manufacturedAt; }
    public String getProductionBatch() { return productionBatch; }
    public String getFactoryCode() { return factoryCode; }
    public String getComponentRootHash() { return componentRootHash; }
    public String getAssetState() { return assetState; }
    public String getRiskFlag() { return riskFlag; }
    public String getOwnershipUserId() { return ownershipUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getVoidedAt() { return voidedAt; }
    public String getVoidedReason() { return voidedReason; }
    public String getVoidedNote() { return voidedNote; }
    public Instant getStolenAt() { return stolenAt; }
    public Instant getLostAt() { return lostAt; }
    public String getRiskReportedBy() { return riskReportedBy; }
    public String getPoliceReportNo() { return policeReportNo; }
}

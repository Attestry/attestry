package io.attestry.workflow.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "workflow_passport_catalog_projection")
public class WorkflowPassportCatalogProjectionEntity {

    @Id
    @Column(name = "passport_id", length = 36)
    private String passportId;

    @Column(name = "asset_id", nullable = false, length = 36)
    private String assetId;

    @Column(name = "serial_number", nullable = false, length = 255)
    private String serialNumber;

    @Column(name = "model_id", length = 100)
    private String modelId;

    @Column(name = "model_name", length = 255)
    private String modelName;

    @Column(name = "production_batch", length = 100)
    private String productionBatch;

    @Column(name = "factory_code", length = 100)
    private String factoryCode;

    @Column(name = "manufactured_at")
    private Instant manufacturedAt;

    @Column(name = "source_event_id", nullable = false, length = 100)
    private String sourceEventId;

    @Column(name = "source_event_version")
    private Long sourceEventVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkflowPassportCatalogProjectionEntity() {
    }

    public String getPassportId() { return passportId; }
    public String getAssetId() { return assetId; }
    public String getSerialNumber() { return serialNumber; }
    public String getModelId() { return modelId; }
    public String getModelName() { return modelName; }
    public String getProductionBatch() { return productionBatch; }
    public String getFactoryCode() { return factoryCode; }
    public Instant getUpdatedAt() { return updatedAt; }
}

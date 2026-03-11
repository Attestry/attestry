package io.attestry.workflow.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "workflow_passport_state_projection")
public class WorkflowPassportStateProjectionEntity {

    @Id
    @Column(name = "passport_id", length = 36)
    private String passportId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "asset_id", nullable = false, length = 36)
    private String assetId;

    @Column(name = "asset_state", nullable = false, length = 30)
    private String assetState;

    @Column(name = "risk_flag", nullable = false, length = 30)
    private String riskFlag;

    @Column(name = "current_owner_id", length = 36)
    private String currentOwnerId;

    @Column(name = "source_event_id", nullable = false, length = 100)
    private String sourceEventId;

    @Column(name = "source_event_version")
    private Long sourceEventVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkflowPassportStateProjectionEntity() {
    }

    public String getPassportId() { return passportId; }
    public String getTenantId() { return tenantId; }
    public String getAssetId() { return assetId; }
    public String getAssetState() { return assetState; }
    public String getRiskFlag() { return riskFlag; }
    public String getCurrentOwnerId() { return currentOwnerId; }
}

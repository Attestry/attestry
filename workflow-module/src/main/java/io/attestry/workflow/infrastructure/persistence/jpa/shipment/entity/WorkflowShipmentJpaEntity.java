package io.attestry.workflow.infrastructure.persistence.jpa.shipment.entity;

import io.attestry.workflow.domain.shipment.model.ShipmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "shipments")
public class WorkflowShipmentJpaEntity {

    @Id
    @Column(name = "shipment_id", nullable = false, length = 36)
    private String shipmentId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "passport_id", nullable = false, length = 36)
    private String passportId;

    @Column(name = "shipment_round", nullable = false)
    private int shipmentRound;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShipmentStatus status;

    @Column(name = "released_at", nullable = false)
    private Instant releasedAt;

    @Column(name = "released_by_user_id", nullable = false, length = 36)
    private String releasedByUserId;

    @Column(name = "released_by_tenant_id", nullable = false, length = 36)
    private String releasedByTenantId;

    @Column(name = "evidence_group_id", nullable = false, length = 36)
    private String evidenceGroupId;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "returned_by_user_id", length = 36)
    private String returnedByUserId;

    @Column(name = "return_evidence_group_id", length = 36)
    private String returnEvidenceGroupId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkflowShipmentJpaEntity() {
    }

    public WorkflowShipmentJpaEntity(
        String shipmentId, String tenantId, String passportId,
        int shipmentRound, ShipmentStatus status,
        Instant releasedAt, String releasedByUserId, String releasedByTenantId,
        String evidenceGroupId,
        Instant returnedAt, String returnedByUserId, String returnEvidenceGroupId,
        Instant createdAt
    ) {
        this.shipmentId = shipmentId;
        this.tenantId = tenantId;
        this.passportId = passportId;
        this.shipmentRound = shipmentRound;
        this.status = status;
        this.releasedAt = releasedAt;
        this.releasedByUserId = releasedByUserId;
        this.releasedByTenantId = releasedByTenantId;
        this.evidenceGroupId = evidenceGroupId;
        this.returnedAt = returnedAt;
        this.returnedByUserId = returnedByUserId;
        this.returnEvidenceGroupId = returnEvidenceGroupId;
        this.createdAt = createdAt;
    }

    public String getShipmentId() { return shipmentId; }
    public String getTenantId() { return tenantId; }
    public String getPassportId() { return passportId; }
    public int getShipmentRound() { return shipmentRound; }
    public ShipmentStatus getStatus() { return status; }
    public Instant getReleasedAt() { return releasedAt; }
    public String getReleasedByUserId() { return releasedByUserId; }
    public String getReleasedByTenantId() { return releasedByTenantId; }
    public String getEvidenceGroupId() { return evidenceGroupId; }
    public Instant getReturnedAt() { return returnedAt; }
    public String getReturnedByUserId() { return returnedByUserId; }
    public String getReturnEvidenceGroupId() { return returnEvidenceGroupId; }
    public Instant getCreatedAt() { return createdAt; }

    public void markReturned(ShipmentStatus status, Instant returnedAt, String returnedByUserId, String returnEvidenceGroupId) {
        this.status = status;
        this.returnedAt = returnedAt;
        this.returnedByUserId = returnedByUserId;
        this.returnEvidenceGroupId = returnEvidenceGroupId;
    }
}

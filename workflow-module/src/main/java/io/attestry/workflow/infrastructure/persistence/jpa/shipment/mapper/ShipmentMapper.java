package io.attestry.workflow.infrastructure.persistence.jpa.shipment.mapper;

import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.infrastructure.persistence.jpa.shipment.entity.WorkflowShipmentJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ShipmentMapper {

    public Shipment toDomain(WorkflowShipmentJpaEntity entity) {
        return new Shipment(
            entity.getShipmentId(),
            entity.getTenantId(),
            entity.getPassportId(),
            entity.getShipmentRound(),
            entity.getStatus(),
            entity.getReleasedAt(),
            entity.getReleasedByUserId(),
            entity.getReleasedByTenantId(),
            entity.getEvidenceGroupId(),
            entity.getReturnedAt(),
            entity.getReturnedByUserId(),
            entity.getReturnEvidenceGroupId(),
            entity.getCreatedAt()
        );
    }

    public WorkflowShipmentJpaEntity toEntity(Shipment domain) {
        return new WorkflowShipmentJpaEntity(
            domain.shipmentId(),
            domain.tenantId(),
            domain.passportId(),
            domain.shipmentRound(),
            domain.status(),
            domain.releasedAt(),
            domain.releasedByUserId(),
            domain.releasedByTenantId(),
            domain.evidenceGroupId(),
            domain.returnedAt(),
            domain.returnedByUserId(),
            domain.returnEvidenceGroupId(),
            domain.createdAt()
        );
    }
}

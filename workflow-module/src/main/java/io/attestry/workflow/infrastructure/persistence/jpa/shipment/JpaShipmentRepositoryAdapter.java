package io.attestry.workflow.infrastructure.persistence.jpa.shipment;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.domain.shipment.model.ShipmentStatus;
import io.attestry.workflow.domain.shipment.repository.ShipmentRepository;
import io.attestry.workflow.infrastructure.persistence.jpa.shipment.entity.WorkflowShipmentJpaEntity;
import io.attestry.workflow.infrastructure.persistence.jpa.shipment.mapper.ShipmentMapper;
import io.attestry.workflow.infrastructure.persistence.jpa.shipment.repository.ShipmentJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaShipmentRepositoryAdapter implements ShipmentRepository {

    private final ShipmentJpaRepository repository;
    private final ShipmentMapper mapper;

    @Override
    public boolean existsActiveReleasedByPassportId(String passportId) {
        return repository.existsByPassportIdAndStatus(passportId, ShipmentStatus.RELEASED);
    }

    @Override
    public int nextShipmentRound(String passportId) {
        return repository.findMaxShipmentRound(passportId) + 1;
    }

    @Override
    public Shipment saveRelease(Shipment shipment) {
        try {
            WorkflowShipmentJpaEntity saved = repository.save(mapper.toEntity(shipment));
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Passport is already RELEASED"
            );
        }
    }

    @Override
    public Shipment saveReturn(Shipment shipment) {
        WorkflowShipmentJpaEntity entity = repository.findById(shipment.shipmentId())
            .orElseThrow(() -> new IllegalStateException("Shipment not found: " + shipment.shipmentId()));

        if (entity.getStatus() != ShipmentStatus.RELEASED) {
            throw new IllegalStateException("Failed to update shipment return state: " + shipment.shipmentId());
        }

        entity.markReturned(
            shipment.status(),
            shipment.returnedAt(),
            shipment.returnedByUserId(),
            shipment.returnEvidenceGroupId()
        );
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Shipment> findByShipmentId(String shipmentId) {
        return repository.findById(shipmentId).map(mapper::toDomain);
    }

    @Override
    public List<Shipment> findByPassportId(String passportId) {
        return repository.findByPassportIdOrderByShipmentRoundDescCreatedAtDesc(passportId)
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Shipment> findLatestByPassportId(String passportId) {
        return repository.findFirstByPassportIdOrderByShipmentRoundDesc(passportId)
            .map(mapper::toDomain);
    }

    @Override
    public List<Shipment> findByTenantId(String tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId)
            .stream().map(mapper::toDomain).toList();
    }
}

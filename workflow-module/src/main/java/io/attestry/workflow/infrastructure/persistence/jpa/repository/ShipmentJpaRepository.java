package io.attestry.workflow.infrastructure.persistence.jpa.repository;

import io.attestry.workflow.domain.shipment.model.ShipmentStatus;
import io.attestry.workflow.infrastructure.persistence.jpa.entity.WorkflowShipmentJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentJpaRepository
    extends JpaRepository<WorkflowShipmentJpaEntity, String>,
            ShipmentProductReadCustomRepository {

    boolean existsByPassportIdAndStatus(String passportId, ShipmentStatus status);

    @Query("SELECT COALESCE(MAX(s.shipmentRound), 0) FROM WorkflowShipmentJpaEntity s WHERE s.passportId = :passportId")
    int findMaxShipmentRound(@Param("passportId") String passportId);

    List<WorkflowShipmentJpaEntity> findByPassportIdOrderByShipmentRoundDescCreatedAtDesc(String passportId);

    Optional<WorkflowShipmentJpaEntity> findFirstByPassportIdOrderByShipmentRoundDesc(String passportId);

    List<WorkflowShipmentJpaEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}

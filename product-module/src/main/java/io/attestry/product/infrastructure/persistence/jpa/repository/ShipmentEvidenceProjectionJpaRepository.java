package io.attestry.product.infrastructure.persistence.jpa.repository;

import io.attestry.product.infrastructure.persistence.jpa.entity.ShipmentEvidenceProjectionId;
import io.attestry.product.infrastructure.persistence.jpa.entity.ShipmentEvidenceProjectionJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentEvidenceProjectionJpaRepository
    extends JpaRepository<ShipmentEvidenceProjectionJpaEntity, ShipmentEvidenceProjectionId> {

    List<ShipmentEvidenceProjectionJpaEntity> findByShipmentIdOrderByEvidenceId(String shipmentId);
}

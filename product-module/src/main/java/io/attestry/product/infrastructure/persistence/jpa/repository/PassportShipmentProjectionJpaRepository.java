package io.attestry.product.infrastructure.persistence.jpa.repository;

import io.attestry.product.infrastructure.persistence.jpa.entity.PassportShipmentProjectionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportShipmentProjectionJpaRepository
    extends JpaRepository<PassportShipmentProjectionJpaEntity, String> {
}

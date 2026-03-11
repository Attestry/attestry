package io.attestry.product.infrastructure.persistence.jpa.repository;

import io.attestry.product.infrastructure.persistence.jpa.entity.PassportDistributionProjectionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportDistributionProjectionJpaRepository
    extends JpaRepository<PassportDistributionProjectionJpaEntity, String> {
}

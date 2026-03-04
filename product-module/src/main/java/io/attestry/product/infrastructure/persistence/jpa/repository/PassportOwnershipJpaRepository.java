package io.attestry.product.infrastructure.persistence.jpa.repository;

import io.attestry.product.infrastructure.persistence.jpa.entity.PassportOwnershipJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportOwnershipJpaRepository extends JpaRepository<PassportOwnershipJpaEntity, String> {
}

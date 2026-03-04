package io.attestry.product.infrastructure.persistence.jpa.repository;

import io.attestry.product.infrastructure.persistence.jpa.entity.ProductPassportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPassportJpaRepository extends JpaRepository<ProductPassportJpaEntity, String> {
}

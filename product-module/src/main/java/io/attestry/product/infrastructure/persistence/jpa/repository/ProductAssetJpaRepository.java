package io.attestry.product.infrastructure.persistence.jpa.repository;

import io.attestry.product.infrastructure.persistence.jpa.entity.ProductAssetJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAssetJpaRepository extends JpaRepository<ProductAssetJpaEntity, String> {

    boolean existsByTenantIdAndSerialNumber(String tenantId, String serialNumber);
}

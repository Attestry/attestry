package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.infrastructure.persistence.jpa.entity.PassportPermissionJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportPermissionJpaRepository extends JpaRepository<PassportPermissionJpaEntity, String> {
    List<PassportPermissionJpaEntity> findByTenantIdAndPassportId(String tenantId, String passportId);

    List<PassportPermissionJpaEntity> findByTenantIdAndRetailGroupId(String tenantId, String retailGroupId);
}

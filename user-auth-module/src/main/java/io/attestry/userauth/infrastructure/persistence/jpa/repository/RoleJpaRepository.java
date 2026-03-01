package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.infrastructure.persistence.jpa.entity.RoleJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, String> {
    Optional<RoleJpaEntity> findByTenantIdIsNullAndCodeAndEnabledTrue(String code);

    List<RoleJpaEntity> findByTenantIdIsNullAndEnabledTrue();
}

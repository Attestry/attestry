package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.infrastructure.persistence.jpa.entity.GroupJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupJpaRepository extends JpaRepository<GroupJpaEntity, String> {
    List<GroupJpaEntity> findByTenantId(String tenantId);
}

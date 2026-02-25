package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.infrastructure.persistence.jpa.entity.OrganizationApplicationJpaEntity;
import io.attestry.userauth.domain.organization.model.GroupType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationApplicationJpaRepository extends JpaRepository<OrganizationApplicationJpaEntity, String> {
    List<OrganizationApplicationJpaEntity> findByType(GroupType type);

    List<OrganizationApplicationJpaEntity> findByTenantId(String tenantId);

    List<OrganizationApplicationJpaEntity> findByTenantIdAndType(String tenantId, GroupType type);
}

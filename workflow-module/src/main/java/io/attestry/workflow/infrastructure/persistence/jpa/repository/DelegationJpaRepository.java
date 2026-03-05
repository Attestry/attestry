package io.attestry.workflow.infrastructure.persistence.jpa.repository;

import io.attestry.workflow.domain.delegation.model.DelegationStatus;
import io.attestry.workflow.infrastructure.persistence.jpa.entity.DelegationJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DelegationJpaRepository extends JpaRepository<DelegationJpaEntity, String> {
    List<DelegationJpaEntity> findBySourceTenantIdOrTargetTenantId(String sourceTenantId, String targetTenantId);

    Optional<DelegationJpaEntity> findBySourceTenantIdAndTargetTenantIdAndResourceTypeAndResourceIdAndPermissionCodeAndStatus(
        String sourceTenantId,
        String targetTenantId,
        String resourceType,
        String resourceId,
        String permissionCode,
        DelegationStatus status
    );

    boolean existsBySourceTenantIdAndTargetTenantIdAndResourceTypeAndResourceIdAndPermissionCodeAndStatus(
        String sourceTenantId,
        String targetTenantId,
        String resourceType,
        String resourceId,
        String permissionCode,
        DelegationStatus status
    );

    List<DelegationJpaEntity> findByResourceTypeAndResourceIdAndStatus(
        String resourceType, String resourceId, DelegationStatus status
    );
}

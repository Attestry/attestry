package io.attestry.workflow.infrastructure.persistence.jpa.repository;

import io.attestry.workflow.domain.delegation.model.DelegationStatus;
import io.attestry.workflow.infrastructure.persistence.jpa.entity.DelegationJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DelegationJpaRepository extends JpaRepository<DelegationJpaEntity, String> {
    List<DelegationJpaEntity> findByBrandTenantIdOrPartnerTenantId(String brandTenantId, String partnerTenantId);

    Optional<DelegationJpaEntity> findByBrandTenantIdAndPartnerTenantIdAndResourceTypeAndResourceIdAndPermissionCodeAndStatus(
        String brandTenantId,
        String partnerTenantId,
        String resourceType,
        String resourceId,
        String permissionCode,
        DelegationStatus status
    );

    boolean existsByBrandTenantIdAndPartnerTenantIdAndResourceTypeAndResourceIdAndPermissionCodeAndStatus(
        String brandTenantId,
        String partnerTenantId,
        String resourceType,
        String resourceId,
        String permissionCode,
        DelegationStatus status
    );
}

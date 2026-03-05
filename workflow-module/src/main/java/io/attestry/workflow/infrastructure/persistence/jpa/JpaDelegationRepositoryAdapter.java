package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.domain.delegation.model.Delegation;
import io.attestry.workflow.domain.delegation.model.DelegationStatus;
import io.attestry.workflow.domain.delegation.repository.DelegationRepository;
import io.attestry.workflow.infrastructure.persistence.jpa.entity.DelegationJpaEntity;
import io.attestry.workflow.infrastructure.persistence.jpa.repository.DelegationJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDelegationRepositoryAdapter implements DelegationRepository {

    private final DelegationJpaRepository repository;

    public JpaDelegationRepositoryAdapter(DelegationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Delegation save(Delegation delegation) {
        long rowVersion = repository.findById(delegation.delegationId())
            .map(DelegationJpaEntity::getRowVersion)
            .orElse(0L);
        return toDomain(repository.save(toEntity(delegation, rowVersion)));
    }

    @Override
    public Optional<Delegation> findById(String delegationId) {
        return repository.findById(delegationId).map(this::toDomain);
    }

    @Override
    public List<Delegation> findByTenantId(String tenantId) {
        return repository.findBySourceTenantIdOrTargetTenantId(tenantId, tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Delegation> findActive(
        String sourceTenantId,
        String targetTenantId,
        String resourceType,
        String resourceId,
        String permissionCode
    ) {
        return repository.findBySourceTenantIdAndTargetTenantIdAndResourceTypeAndResourceIdAndPermissionCodeAndStatus(
            sourceTenantId,
            targetTenantId,
            resourceType,
            resourceId,
            permissionCode,
            DelegationStatus.ACTIVE
        ).map(this::toDomain);
    }

    @Override
    public boolean existsActive(
        String sourceTenantId,
        String targetTenantId,
        String resourceType,
        String resourceId,
        String permissionCode
    ) {
        return repository.existsBySourceTenantIdAndTargetTenantIdAndResourceTypeAndResourceIdAndPermissionCodeAndStatus(
            sourceTenantId,
            targetTenantId,
            resourceType,
            resourceId,
            permissionCode,
            DelegationStatus.ACTIVE
        );
    }

    @Override
    public List<Delegation> findActiveByResourceId(String resourceType, String resourceId) {
        return repository.findByResourceTypeAndResourceIdAndStatus(resourceType, resourceId, DelegationStatus.ACTIVE)
            .stream().map(this::toDomain).toList();
    }

    private Delegation toDomain(DelegationJpaEntity entity) {
        return new Delegation(
            entity.getDelegationId(),
            entity.getPartnerLinkId(),
            entity.getSourceTenantId(),
            entity.getTargetTenantId(),
            entity.getResourceType(),
            entity.getResourceId(),
            entity.getPermissionCode(),
            entity.getStatus(),
            entity.getExpiresAt(),
            entity.getGrantedByUserId(),
            entity.getCreatedAt(),
            entity.getRevokedByUserId(),
            entity.getRevokedAt(),
            entity.getReason()
        );
    }

    private DelegationJpaEntity toEntity(Delegation delegation, long rowVersion) {
        return new DelegationJpaEntity(
            delegation.delegationId(),
            delegation.partnerLinkId(),
            delegation.sourceTenantId(),
            delegation.targetTenantId(),
            delegation.resourceType(),
            delegation.resourceId(),
            delegation.permissionCode(),
            delegation.status(),
            delegation.expiresAt(),
            delegation.grantedByUserId(),
            delegation.createdAt(),
            delegation.revokedByUserId(),
            delegation.revokedAt(),
            delegation.reason(),
            rowVersion
        );
    }
}

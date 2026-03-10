package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.domain.delegation.model.Delegation;
import io.attestry.workflow.domain.delegation.model.DelegationStatus;
import io.attestry.workflow.domain.delegation.repository.DelegationRepository;
import io.attestry.workflow.infrastructure.persistence.jpa.entity.DelegationJpaEntity;
import io.attestry.workflow.infrastructure.persistence.jpa.mapper.DelegationMapper;
import io.attestry.workflow.infrastructure.persistence.jpa.repository.DelegationJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDelegationRepositoryAdapter implements DelegationRepository {

    private final DelegationJpaRepository repository;
    private final DelegationMapper mapper;

    public JpaDelegationRepositoryAdapter(DelegationJpaRepository repository, DelegationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Delegation save(Delegation delegation) {
        long rowVersion = repository.findById(delegation.delegationId())
            .map(DelegationJpaEntity::getRowVersion)
            .orElse(0L);
        return mapper.toDomain(repository.save(mapper.toEntity(delegation, rowVersion)));
    }

    @Override
    public Optional<Delegation> findById(String delegationId) {
        return repository.findById(delegationId).map(mapper::toDomain);
    }

    @Override
    public List<Delegation> findByTenantId(String tenantId) {
        return repository.findBySourceTenantIdOrTargetTenantId(tenantId, tenantId).stream().map(mapper::toDomain).toList();
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
        ).map(mapper::toDomain);
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
            .stream().map(mapper::toDomain).toList();
    }
}

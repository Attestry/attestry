package io.attestry.userauth.infrastructure.persistence.jpa.repository.adapter;

import io.attestry.userauth.domain.tenant.model.Tenant;
import io.attestry.userauth.domain.tenant.model.TenantStatus;
import io.attestry.userauth.domain.tenant.model.TenantType;
import io.attestry.userauth.application.port.TenantRepositoryPort;
import io.attestry.userauth.infrastructure.persistence.jpa.mapper.TenantMapper;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.TenantJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class JpaTenantRepositoryAdapter implements TenantRepositoryPort {

    private final TenantJpaRepository tenantJpaRepository;
    private final TenantMapper tenantMapper;

    @Override
    public Tenant save(Tenant tenant) {
        tenantJpaRepository.save(tenantMapper.toEntity(tenant));
        return tenant;
    }

    @Override
    public Optional<Tenant> findById(String tenantId) {
        return tenantJpaRepository.findById(tenantId).map(tenantMapper::toDomain);
    }

    @Override
    public Page<Tenant> findPage(TenantType type, TenantStatus status, Pageable pageable) {
        Specification<TenantJpaEntity> specification = (root, query, cb) -> cb.conjunction();
        if (type != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return tenantJpaRepository.findAll(specification, pageable).map(tenantMapper::toDomain);
    }
}

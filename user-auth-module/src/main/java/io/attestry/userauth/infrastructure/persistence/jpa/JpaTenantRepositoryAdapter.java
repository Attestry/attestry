package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.domain.organization.model.Tenant;
import io.attestry.userauth.domain.organization.repository.TenantRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.TenantJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.TenantJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTenantRepositoryAdapter implements TenantRepository {

    private final TenantJpaRepository tenantJpaRepository;

    public JpaTenantRepositoryAdapter(TenantJpaRepository tenantJpaRepository) {
        this.tenantJpaRepository = tenantJpaRepository;
    }

    @Override
    public Tenant save(Tenant tenant) {
        tenantJpaRepository.save(new TenantJpaEntity(
            tenant.tenantId(),
            tenant.name(),
            tenant.region(),
            tenant.address(),
            tenant.type(),
            tenant.status()
        ));
        return tenant;
    }

    @Override
    public Optional<Tenant> findById(String tenantId) {
        return tenantJpaRepository.findById(tenantId).map(entity ->
            Tenant.reconstitute(
                entity.getTenantId(),
                entity.getName(),
                entity.getRegion(),
                entity.getAddress(),
                entity.getType(),
                entity.getStatus()
            )
        );
    }
}

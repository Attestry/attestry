package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.TenantRepositoryPort;
import io.attestry.userauth.domain.organization.model.Tenant;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.TenantJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.TenantJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTenantRepositoryAdapter implements TenantRepositoryPort {

    private final TenantJpaRepository repository;

    public JpaTenantRepositoryAdapter(TenantJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Tenant save(Tenant tenant) {
        TenantJpaEntity saved = repository.save(new TenantJpaEntity(
            tenant.tenantId(),
            tenant.name(),
            tenant.region(),
            tenant.status()
        ));
        return new Tenant(saved.getTenantId(), saved.getName(), saved.getRegion(), saved.getStatus());
    }
}

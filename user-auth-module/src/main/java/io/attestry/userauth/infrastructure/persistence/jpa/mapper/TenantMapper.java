package io.attestry.userauth.infrastructure.persistence.jpa.mapper;

import io.attestry.commonlib.infrastructure.DomainMapper;
import io.attestry.userauth.domain.tenant.model.Tenant;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.TenantJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TenantMapper implements DomainMapper<Tenant, TenantJpaEntity> {
    @Override
    public Tenant toDomain(TenantJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Tenant.reconstitute(
            entity.getTenantId(),
            entity.getName(),
            entity.getRegion(),
            entity.getAddress(),
            entity.getType(),
            entity.getStatus()
        );
    }

    @Override
    public TenantJpaEntity toEntity(Tenant domain) {
        if (domain == null) {
            return null;
        }
        return new TenantJpaEntity(
            domain.tenantId(),
            domain.name(),
            domain.region(),
            domain.address(),
            domain.type(),
            domain.status()
        );
    }
}

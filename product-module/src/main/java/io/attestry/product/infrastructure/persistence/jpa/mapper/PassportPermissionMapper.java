package io.attestry.product.infrastructure.persistence.jpa.mapper;

import io.attestry.commonlib.infrastructure.DomainMapper;
import io.attestry.product.domain.permission.model.PassportPermission;
import io.attestry.product.domain.permission.model.PermissionScope;
import io.attestry.product.domain.permission.model.PermissionStatus;
import io.attestry.product.infrastructure.persistence.jpa.entity.PassportPermissionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PassportPermissionMapper implements DomainMapper<PassportPermission, PassportPermissionJpaEntity> {

    @Override
    public PassportPermission toDomain(PassportPermissionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return PassportPermission.reconstitute(
            entity.getPermissionId(),
            entity.getPassportId(),
            entity.getSellerTenantId(),
            PermissionScope.valueOf(entity.getScope()),
            PermissionStatus.valueOf(entity.getStatus()),
            entity.getExpiresAt(),
            entity.getCreatedAt()
        );
    }

    @Override
    public PassportPermissionJpaEntity toEntity(PassportPermission permission) {
        if (permission == null) {
            return null;
        }
        return new PassportPermissionJpaEntity(
            permission.getPermissionId(),
            permission.getPassportId(),
            permission.getSellerTenantId(),
            permission.getScope().name(),
            permission.getStatus().name(),
            permission.getExpiresAt(),
            permission.getCreatedAt()
        );
    }
}

package io.attestry.product.infrastructure.persistence.jpa.mapper;

import io.attestry.commonlib.infrastructure.DomainMapper;
import io.attestry.product.domain.ownership.model.PassportOwnership;
import io.attestry.product.infrastructure.persistence.jpa.entity.PassportOwnershipJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PassportOwnershipMapper implements DomainMapper<PassportOwnership, PassportOwnershipJpaEntity> {

    @Override
    public PassportOwnership toDomain(PassportOwnershipJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return PassportOwnership.reconstitute(
            entity.getPassportId(),
            entity.getOwnerId(),
            entity.getUpdatedAt()
        );
    }

    @Override
    public PassportOwnershipJpaEntity toEntity(PassportOwnership ownership) {
        if (ownership == null) {
            return null;
        }
        return new PassportOwnershipJpaEntity(
            ownership.getPassportId(),
            ownership.getOwnerId(),
            ownership.getUpdatedAt()
        );
    }
}

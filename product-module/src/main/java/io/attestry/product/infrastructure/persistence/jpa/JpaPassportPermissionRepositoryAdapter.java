package io.attestry.product.infrastructure.persistence.jpa;

import io.attestry.product.domain.permission.model.PassportPermission;
import io.attestry.product.domain.permission.model.PermissionScope;
import io.attestry.product.domain.permission.model.PermissionStatus;
import io.attestry.product.domain.permission.repository.PassportPermissionRepository;
import io.attestry.product.infrastructure.persistence.jpa.entity.PassportPermissionJpaEntity;
import io.attestry.product.infrastructure.persistence.jpa.repository.PassportPermissionJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPassportPermissionRepositoryAdapter implements PassportPermissionRepository {

    private final PassportPermissionJpaRepository jpaRepository;

    public JpaPassportPermissionRepositoryAdapter(PassportPermissionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<PassportPermission> findById(String permissionId) {
        return jpaRepository.findById(permissionId).map(this::toDomain);
    }

    @Override
    public List<PassportPermission> findByPassportId(String passportId) {
        return jpaRepository.findByPassportId(passportId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public PassportPermission save(PassportPermission permission) {
        PassportPermissionJpaEntity entity = toEntity(permission);
        PassportPermissionJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public boolean existsActiveByPassportAndSellerGroup(String passportId, String sellerGroupId) {
        return jpaRepository.existsActiveByPassportIdAndSellerGroupId(passportId, sellerGroupId);
    }

    private PassportPermissionJpaEntity toEntity(PassportPermission permission) {
        return new PassportPermissionJpaEntity(
            permission.getPermissionId(),
            permission.getPassportId(),
            permission.getSellerGroupId(),
            permission.getScope().name(),
            permission.getStatus().name(),
            permission.getExpiresAt(),
            permission.getCreatedAt()
        );
    }

    private PassportPermission toDomain(PassportPermissionJpaEntity entity) {
        return PassportPermission.reconstitute(
            entity.getPermissionId(),
            entity.getPassportId(),
            entity.getSellerGroupId(),
            PermissionScope.valueOf(entity.getScope()),
            PermissionStatus.valueOf(entity.getStatus()),
            entity.getExpiresAt(),
            entity.getCreatedAt()
        );
    }
}

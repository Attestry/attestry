package io.attestry.product.infrastructure.persistence.jpa.permission;

import io.attestry.product.application.port.permission.PassportPermissionPort;
import io.attestry.product.domain.permission.model.PassportPermission;
import io.attestry.product.infrastructure.persistence.jpa.mapper.PassportPermissionMapper;
import io.attestry.product.infrastructure.persistence.jpa.repository.PassportPermissionJpaRepository;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class JpaPassportPermissionRepositoryAdapter implements PassportPermissionPort {

    private final PassportPermissionJpaRepository jpaRepository;
    private final PassportPermissionMapper mapper;

    @Override
    public Optional<PassportPermission> findById(String permissionId) {
        return jpaRepository.findById(permissionId).map(mapper::toDomain);
    }

    @Override
    public List<PassportPermission> findByPassportId(String passportId) {
        return jpaRepository.findByPassportId(passportId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public PassportPermission save(PassportPermission permission) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(permission)));
    }

    @Override
    public boolean existsActiveByPassportAndSellerTenant(String passportId, String sellerTenantId) {
        return jpaRepository.existsActiveByPassportIdAndSellerTenantId(passportId, sellerTenantId);
    }
}

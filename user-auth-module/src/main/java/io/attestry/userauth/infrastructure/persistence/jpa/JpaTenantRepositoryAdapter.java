package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.domain.organization.model.Group;
import io.attestry.userauth.domain.organization.model.Tenant;
import io.attestry.userauth.domain.organization.repository.TenantRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.GroupJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.TenantJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.GroupJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.TenantJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTenantRepositoryAdapter implements TenantRepository {

    private final TenantJpaRepository tenantJpaRepository;
    private final GroupJpaRepository groupJpaRepository;

    public JpaTenantRepositoryAdapter(TenantJpaRepository tenantJpaRepository, GroupJpaRepository groupJpaRepository) {
        this.tenantJpaRepository = tenantJpaRepository;
        this.groupJpaRepository = groupJpaRepository;
    }

    @Override
    public Tenant save(Tenant tenant) {
        tenantJpaRepository.save(new TenantJpaEntity(
            tenant.tenantId(),
            tenant.name(),
            tenant.region(),
            tenant.status()
        ));
        for (Group group : tenant.groups()) {
            groupJpaRepository.save(new GroupJpaEntity(
                group.groupId(),
                group.tenantId(),
                group.type(),
                group.status()
            ));
        }
        return tenant;
    }

    @Override
    public Optional<Tenant> findById(String tenantId) {
        return tenantJpaRepository.findById(tenantId).map(entity -> {
            List<Group> groups = groupJpaRepository.findByTenantId(tenantId).stream()
                .map(this::toGroupDomain)
                .toList();
            return Tenant.reconstitute(
                entity.getTenantId(),
                entity.getName(),
                entity.getRegion(),
                entity.getStatus(),
                groups
            );
        });
    }

    private Group toGroupDomain(GroupJpaEntity entity) {
        return Group.reconstitute(entity.getGroupId(), entity.getTenantId(), entity.getType(), entity.getStatus());
    }
}

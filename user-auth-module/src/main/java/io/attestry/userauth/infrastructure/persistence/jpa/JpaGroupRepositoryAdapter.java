package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.GroupRepositoryPort;
import io.attestry.userauth.domain.organization.model.Group;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.GroupJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.GroupJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class JpaGroupRepositoryAdapter implements GroupRepositoryPort {

    private final GroupJpaRepository repository;

    public JpaGroupRepositoryAdapter(GroupJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Group save(Group group) {
        GroupJpaEntity saved = repository.save(new GroupJpaEntity(
            group.groupId(),
            group.tenantId(),
            group.type(),
            group.status()
        ));
        return new Group(saved.getGroupId(), saved.getTenantId(), saved.getType(), saved.getStatus());
    }
}

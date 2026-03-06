package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.MembershipJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaMembershipRepositoryAdapter implements MembershipRepositoryPort {

    private final MembershipJpaRepository repository;

    public JpaMembershipRepositoryAdapter(MembershipJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Membership> findByUserId(String userId) {
        return repository.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Membership> findByUserIdAndTenantId(String userId, String tenantId) {
        return repository.findByUserIdAndTenantId(userId, tenantId).map(this::toDomain);
    }

    private Membership toDomain(MembershipJpaEntity entity) {
        return Membership.reconstitute(
            entity.getMembershipId(),
            entity.getUserId(),
            entity.getTenantId(),
            entity.getTenantType(),
            entity.getRole(),
            entity.getStatus(),
            entity.getTenantStatus(),
            Set.of()
        );
    }
}

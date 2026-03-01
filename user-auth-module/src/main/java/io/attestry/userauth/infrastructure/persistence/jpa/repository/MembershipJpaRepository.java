package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipJpaRepository extends JpaRepository<MembershipJpaEntity, String> {
    List<MembershipJpaEntity> findByUserId(String userId);
    List<MembershipJpaEntity> findByTenantId(String tenantId);
    List<MembershipJpaEntity> findByGroupId(String groupId);

    Optional<MembershipJpaEntity> findByMembershipIdAndTenantId(String membershipId, String tenantId);

    Optional<MembershipJpaEntity> findByUserIdAndTenantIdAndGroupId(String userId, String tenantId, String groupId);
}

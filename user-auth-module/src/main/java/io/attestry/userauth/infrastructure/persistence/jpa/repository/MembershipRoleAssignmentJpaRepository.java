package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipRoleAssignmentJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRoleAssignmentJpaRepository extends JpaRepository<MembershipRoleAssignmentJpaEntity, String> {
    Optional<MembershipRoleAssignmentJpaEntity> findByMembershipId(String membershipId);
}

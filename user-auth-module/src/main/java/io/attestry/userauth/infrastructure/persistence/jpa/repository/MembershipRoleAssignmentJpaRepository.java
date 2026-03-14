package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipRoleAssignmentJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipRoleAssignmentJpaRepository extends JpaRepository<MembershipRoleAssignmentJpaEntity, String> {
    Optional<MembershipRoleAssignmentJpaEntity> findByMembershipId(String membershipId);

    Optional<MembershipRoleAssignmentJpaEntity> findByMembershipIdAndRoleId(String membershipId, String roleId);

    List<MembershipRoleAssignmentJpaEntity> findAllByMembershipId(String membershipId);

    void deleteByMembershipIdAndRoleId(String membershipId, String roleId);

    @Query(
        value = """
            SELECT r.code
            FROM membership_role_assignments mra
            JOIN roles r ON r.role_id = mra.role_id
            WHERE mra.membership_id = :membershipId
              AND r.enabled = TRUE
            """,
        nativeQuery = true
    )
    List<String> findRoleCodesByMembershipId(@Param("membershipId") String membershipId);
}

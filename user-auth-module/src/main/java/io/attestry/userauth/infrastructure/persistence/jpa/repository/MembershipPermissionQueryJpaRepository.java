package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipPermissionQueryJpaRepository extends JpaRepository<MembershipJpaEntity, String> {

    @Query(
        value = """
            SELECT permission_code
            FROM membership_effective_permissions
            WHERE membership_id = :membershipId
            ORDER BY permission_code
            """,
        nativeQuery = true
    )
    List<String> findPermissionCodesByMembershipId(@Param("membershipId") String membershipId);

    @Query(
        value = """
            SELECT p.code
            FROM roles r
            JOIN role_permissions rp ON rp.role_id = r.role_id
            JOIN permissions p ON p.permission_id = rp.permission_id
            WHERE r.tenant_id IS NULL
              AND r.code = :roleCode
              AND r.enabled = TRUE
              AND p.enabled = TRUE
            """,
        nativeQuery = true
    )
    List<String> findPermissionCodesByGlobalRoleCode(@Param("roleCode") String roleCode);

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

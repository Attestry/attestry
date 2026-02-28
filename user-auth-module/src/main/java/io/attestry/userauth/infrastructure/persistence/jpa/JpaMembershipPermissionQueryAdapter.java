package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.MembershipPermissionQueryPort;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMembershipPermissionQueryAdapter implements MembershipPermissionQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public JpaMembershipPermissionQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    // TODO("jpa로 리팩토링")
    @Override
    public Set<String> findPermissionCodesByMembershipId(String membershipId) {
        return new LinkedHashSet<>(jdbcTemplate.queryForList(
            """
                SELECT p.code
                FROM membership_role_assignments mra
                JOIN roles r ON r.role_id = mra.role_id
                JOIN role_permissions rp ON rp.role_id = r.role_id
                JOIN permissions p ON p.permission_id = rp.permission_id
                WHERE mra.membership_id = ?
                  AND r.enabled = TRUE
                  AND p.enabled = TRUE
                """,
            String.class,
            membershipId
        ));
    }

    @Override
    public Set<String> findPermissionCodesByGlobalRoleCode(String roleCode) {
        return new LinkedHashSet<>(jdbcTemplate.queryForList(
            """
                SELECT p.code
                FROM roles r
                JOIN role_permissions rp ON rp.role_id = r.role_id
                JOIN permissions p ON p.permission_id = rp.permission_id
                WHERE r.tenant_id IS NULL
                  AND r.code = ?
                  AND r.enabled = TRUE
                  AND p.enabled = TRUE
                """,
            String.class,
            roleCode
        ));
    }
}

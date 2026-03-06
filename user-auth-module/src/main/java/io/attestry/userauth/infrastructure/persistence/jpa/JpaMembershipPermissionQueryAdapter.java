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

    @Override
    public Set<String> findPermissionCodesByMembershipId(String membershipId) {
        return new LinkedHashSet<>(jdbcTemplate.queryForList(
            """
                SELECT DISTINCT effective.code
                FROM (
                    SELECT p.code
                    FROM membership_role_assignments mra
                    JOIN roles r ON r.role_id = mra.role_id
                    JOIN role_permissions rp ON rp.role_id = r.role_id
                    JOIN permissions p ON p.permission_id = rp.permission_id
                    WHERE mra.membership_id = ?
                      AND r.enabled = TRUE
                      AND p.enabled = TRUE

                    UNION

                    SELECT p.code
                    FROM memberships m
                    JOIN membership_role_assignments mra ON mra.membership_id = m.membership_id
                    JOIN roles r ON r.role_id = mra.role_id
                    JOIN tenant_role_template_bindings trtb
                        ON trtb.tenant_id = m.tenant_id
                       AND trtb.role_code = r.code
                       AND trtb.enabled = TRUE
                    JOIN permission_templates t
                        ON t.template_id = trtb.template_id
                       AND t.enabled = TRUE
                    JOIN template_permissions tp ON tp.template_id = t.template_id
                    JOIN permissions p ON p.permission_id = tp.permission_id
                    WHERE m.membership_id = ?
                      AND r.enabled = TRUE
                      AND p.enabled = TRUE

                    UNION

                    SELECT p.code
                    FROM membership_permission_overrides mpo
                    JOIN permissions p ON p.permission_id = mpo.permission_id
                    WHERE mpo.membership_id = ?
                      AND mpo.effect = 'ALLOW'
                      AND p.enabled = TRUE
                ) effective
                WHERE effective.code NOT IN (
                    SELECT p.code
                    FROM membership_permission_overrides mpo
                    JOIN permissions p ON p.permission_id = mpo.permission_id
                    WHERE mpo.membership_id = ?
                      AND mpo.effect = 'DENY'
                      AND p.enabled = TRUE
                )
                """,
            String.class,
            membershipId,
            membershipId,
            membershipId,
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

    @Override
    public Set<String> findRoleCodesByMembershipId(String membershipId) {
        return new LinkedHashSet<>(jdbcTemplate.queryForList(
            """
                SELECT r.code
                FROM membership_role_assignments mra
                JOIN roles r ON r.role_id = mra.role_id
                WHERE mra.membership_id = ?
                  AND r.enabled = TRUE
                """,
            String.class,
            membershipId
        ));
    }
}

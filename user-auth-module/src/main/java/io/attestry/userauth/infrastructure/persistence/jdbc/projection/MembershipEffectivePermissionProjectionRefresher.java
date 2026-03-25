package io.attestry.userauth.infrastructure.persistence.jdbc.projection;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipEffectivePermissionProjectionRefresher {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void refreshMembership(String membershipId) {
        if (membershipId == null || membershipId.isBlank()) {
            return;
        }
        jdbcTemplate.getJdbcOperations().update(
            "DELETE FROM membership_effective_permissions WHERE membership_id = ?",
            membershipId
        );
        jdbcTemplate.getJdbcOperations().update(
            """
                INSERT INTO membership_effective_permissions (
                    membership_id,
                    tenant_id,
                    permission_code,
                    updated_at
                )
                SELECT DISTINCT
                    a.membership_id,
                    a.tenant_id,
                    a.code,
                    ?
                FROM (
                    SELECT mra.membership_id, m.tenant_id, p.code
                    FROM membership_role_assignments mra
                    JOIN memberships m ON m.membership_id = mra.membership_id
                    JOIN roles r ON r.role_id = mra.role_id
                    JOIN role_permissions rp ON rp.role_id = r.role_id
                    JOIN permissions p ON p.permission_id = rp.permission_id
                    WHERE mra.membership_id = ?
                      AND r.enabled = TRUE
                      AND p.enabled = TRUE

                    UNION ALL

                    SELECT m.membership_id, m.tenant_id, p.code
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

                    UNION ALL

                    SELECT mpo.membership_id, m.tenant_id, p.code
                    FROM membership_permission_overrides mpo
                    JOIN memberships m ON m.membership_id = mpo.membership_id
                    JOIN permissions p ON p.permission_id = mpo.permission_id
                    WHERE mpo.membership_id = ?
                      AND mpo.effect = 'ALLOW'
                      AND p.enabled = TRUE
                ) a
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM (
                        SELECT mpo.membership_id, p.code
                        FROM membership_permission_overrides mpo
                        JOIN permissions p ON p.permission_id = mpo.permission_id
                        WHERE mpo.membership_id = ?
                          AND mpo.effect = 'DENY'
                          AND p.enabled = TRUE
                    ) d
                    WHERE d.membership_id = a.membership_id
                      AND d.code = a.code
                )
            """,
            OffsetDateTime.now(ZoneOffset.UTC),
            membershipId,
            membershipId,
            membershipId,
            membershipId
        );
    }

    public void refreshTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return;
        }
        List<String> membershipIds = jdbcTemplate.getJdbcOperations().queryForList(
            "SELECT membership_id FROM memberships WHERE tenant_id = ?",
            String.class,
            tenantId
        );
        for (String membershipId : membershipIds) {
            refreshMembership(membershipId);
        }
    }

    public void refreshAll() {
        List<String> membershipIds = jdbcTemplate.getJdbcOperations().queryForList(
            "SELECT membership_id FROM memberships",
            String.class
        );
        for (String membershipId : membershipIds) {
            refreshMembership(membershipId);
        }
    }

    public void refreshByTemplateId(String templateId) {
        if (templateId == null || templateId.isBlank()) {
            return;
        }
        List<String> membershipIds = jdbcTemplate.getJdbcOperations().queryForList(
            """
                SELECT DISTINCT m.membership_id
                FROM memberships m
                JOIN membership_role_assignments mra ON mra.membership_id = m.membership_id
                JOIN roles r ON r.role_id = mra.role_id
                JOIN tenant_role_template_bindings trtb
                    ON trtb.tenant_id = m.tenant_id
                   AND trtb.role_code = r.code
                   AND trtb.enabled = TRUE
                WHERE trtb.template_id = ?
            """,
            String.class,
            templateId
        );
        for (String membershipId : membershipIds) {
            refreshMembership(membershipId);
        }
    }
}

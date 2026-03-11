package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.domain.authorization.policy.PermissionCatalog;
import io.attestry.userauth.domain.authorization.policy.SystemPermissionTemplateCatalog;
import io.attestry.userauth.domain.authorization.policy.SystemPermissionTemplateCatalog.TemplateDefinition;
import io.attestry.userauth.infrastructure.persistence.jpa.membership.MembershipEffectivePermissionProjectionRefresher;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RbacCatalogProjectionSync implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionCatalog permissionCatalog;
    private final MembershipEffectivePermissionProjectionRefresher permissionProjectionRefresher;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        syncPermissions();
        syncDefaultTemplates();
        permissionProjectionRefresher.refreshAll();
    }

    private void syncPermissions() {
        for (PermissionCatalog.PermissionDefinition definition : permissionCatalog.all()) {
            int updated = jdbcTemplate.update(
                """
                    UPDATE permissions
                    SET name = ?,
                        description = ?,
                        resource_type = ?,
                        action = ?,
                        enabled = ?
                    WHERE code = ?
                    """,
                definition.name(),
                definition.description(),
                definition.resourceType(),
                definition.action(),
                definition.enabled(),
                definition.code()
            );
            if (updated == 0) {
                jdbcTemplate.update(
                    """
                        INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                    definition.permissionId(),
                    definition.code(),
                    definition.name(),
                    definition.description(),
                    definition.resourceType(),
                    definition.action(),
                    definition.enabled()
                );
            }
        }
    }

    private void syncDefaultTemplates() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Map<String, TemplateDefinition> templates = SystemPermissionTemplateCatalog.defaults();
        for (TemplateDefinition definition : templates.values()) {
            String templateId = upsertTemplate(definition, now);
            syncTemplatePermissions(templateId, definition.permissionCodes());
        }
    }

    private String upsertTemplate(TemplateDefinition definition, OffsetDateTime now) {
        int updated = jdbcTemplate.update(
            """
                UPDATE permission_templates
                SET name = ?,
                    description = ?,
                    enabled = TRUE,
                    updated_at = ?
                WHERE code = ?
                """,
            definition.name(),
            definition.description(),
            now,
            definition.code()
        );
        if (updated == 0) {
            jdbcTemplate.update(
                """
                    INSERT INTO permission_templates (template_id, code, name, description, enabled, created_by_user_id, created_at)
                    VALUES (?, ?, ?, ?, TRUE, NULL, ?)
                    """,
                definition.templateId(),
                definition.code(),
                definition.name(),
                definition.description(),
                now
            );
        }
        return jdbcTemplate.queryForObject(
            "SELECT template_id FROM permission_templates WHERE code = ?",
            String.class,
            definition.code()
        );
    }

    private void syncTemplatePermissions(String templateId, List<String> permissionCodes) {
        // Batch resolve permission IDs
        List<String> desiredPermissionIds = permissionCodes.stream()
            .map(code -> jdbcTemplate.queryForObject(
                "SELECT permission_id FROM permissions WHERE code = ? AND enabled = TRUE",
                String.class,
                code
            ))
            .filter(id -> id != null)
            .toList();

        // Insert missing bindings (PK conflict = already exists, skip)
        for (String permissionId : desiredPermissionIds) {
            jdbcTemplate.update(
                """
                    INSERT INTO template_permissions (template_id, permission_id)
                    SELECT ?, ?
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM template_permissions
                        WHERE template_id = ?
                          AND permission_id = ?
                    )
                    """,
                templateId,
                permissionId,
                templateId,
                permissionId
            );
        }

        // Remove orphan bindings
        if (desiredPermissionIds.isEmpty()) {
            jdbcTemplate.update(
                "DELETE FROM template_permissions WHERE template_id = ?",
                templateId
            );
        } else {
            String placeholders = desiredPermissionIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));
            Object[] params = new Object[desiredPermissionIds.size() + 1];
            params[0] = templateId;
            for (int i = 0; i < desiredPermissionIds.size(); i++) {
                params[i + 1] = desiredPermissionIds.get(i);
            }
            jdbcTemplate.update(
                "DELETE FROM template_permissions WHERE template_id = ? AND permission_id NOT IN (" + placeholders + ")",
                params
            );
        }
    }
}

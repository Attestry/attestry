package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.domain.authorization.policy.PermissionCatalog;
import io.attestry.userauth.domain.authorization.policy.SystemPermissionTemplateCatalog;
import io.attestry.userauth.domain.authorization.policy.SystemPermissionTemplateCatalog.TemplateDefinition;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RbacCatalogProjectionSync implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionCatalog permissionCatalog;

    public RbacCatalogProjectionSync(JdbcTemplate jdbcTemplate, PermissionCatalog permissionCatalog) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionCatalog = permissionCatalog;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        syncPermissions();
        syncDefaultTemplates();
    }

    private void syncPermissions() {
        for (PermissionCatalog.PermissionDefinition definition : permissionCatalog.all()) {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM permissions WHERE code = ?",
                Integer.class,
                definition.code()
            );
            if (count == null || count == 0) {
                jdbcTemplate.update(
                    """
                        INSERT INTO permissions (
                            permission_id,
                            code,
                            name,
                            description,
                            resource_type,
                            action,
                            enabled
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                    definition.permissionId(),
                    definition.code(),
                    definition.name(),
                    definition.description(),
                    definition.resourceType(),
                    definition.action(),
                    definition.enabled()
                );
            } else {
                jdbcTemplate.update(
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
            }
        }
    }

    private void syncDefaultTemplates() {
        Map<String, TemplateDefinition> templates = SystemPermissionTemplateCatalog.defaults();
        for (TemplateDefinition definition : templates.values()) {
            String templateId = resolveTemplateId(definition);
            jdbcTemplate.update(
                """
                    UPDATE permission_templates
                    SET name = ?,
                        description = ?,
                        enabled = TRUE,
                        updated_at = ?
                    WHERE template_id = ?
                    """,
                definition.name(),
                definition.description(),
                Timestamp.from(java.time.Instant.now()),
                templateId
            );
            syncTemplatePermissions(templateId, definition.permissionCodes());
        }
    }

    private String resolveTemplateId(TemplateDefinition definition) {
        List<String> ids = jdbcTemplate.queryForList(
            "SELECT template_id FROM permission_templates WHERE code = ?",
            String.class,
            definition.code()
        );
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        jdbcTemplate.update(
            """
                INSERT INTO permission_templates (
                    template_id,
                    code,
                    name,
                    description,
                    enabled,
                    created_by_user_id,
                    created_at
                ) VALUES (?, ?, ?, ?, TRUE, NULL, ?)
                """,
            definition.templateId(),
            definition.code(),
            definition.name(),
            definition.description(),
            Timestamp.from(java.time.Instant.now())
        );
        return definition.templateId();
    }

    private void syncTemplatePermissions(String templateId, List<String> permissionCodes) {
        List<String> permissionIds = new ArrayList<>();
        for (String code : permissionCodes) {
            String permissionId = jdbcTemplate.queryForObject(
                "SELECT permission_id FROM permissions WHERE code = ? AND enabled = TRUE",
                String.class,
                code
            );
            if (permissionId != null) {
                permissionIds.add(permissionId);
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
        }

        List<String> existingPermissionIds = jdbcTemplate.queryForList(
            "SELECT permission_id FROM template_permissions WHERE template_id = ?",
            String.class,
            templateId
        );
        for (String existingPermissionId : existingPermissionIds) {
            if (!permissionIds.contains(existingPermissionId)) {
                jdbcTemplate.update(
                    "DELETE FROM template_permissions WHERE template_id = ? AND permission_id = ?",
                    templateId,
                    existingPermissionId
                );
            }
        }
    }
}

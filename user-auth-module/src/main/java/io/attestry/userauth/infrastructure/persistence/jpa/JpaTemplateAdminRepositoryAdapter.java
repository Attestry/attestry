package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.TemplateAdminRepositoryPort;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.auth.policy.PermissionCatalog;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTemplateAdminRepositoryAdapter implements TemplateAdminRepositoryPort {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionCatalog permissionCatalog;

    public JpaTemplateAdminRepositoryAdapter(JdbcTemplate jdbcTemplate, PermissionCatalog permissionCatalog) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionCatalog = permissionCatalog;
    }

    @Override
    public Optional<PermissionTemplateView> findTemplateByCode(String templateCode) {
        List<PermissionTemplateView> rows = jdbcTemplate.query(
            """
                SELECT template_id, code, name, description, enabled
                FROM permission_templates
                WHERE code = ?
                """,
            (rs, rowNum) -> mapTemplate(rs, findPermissionCodesByTemplateId(rs.getString("template_id"))),
            templateCode
        );
        return rows.stream().findFirst();
    }

    @Override
    public List<PermissionTemplateView> findAllTemplates() {
        return jdbcTemplate.query(
            """
                SELECT template_id, code, name, description, enabled
                FROM permission_templates
                ORDER BY code
                """,
            (rs, rowNum) -> mapTemplate(rs, findPermissionCodesByTemplateId(rs.getString("template_id")))
        );
    }

    @Override
    public PermissionTemplateView createTemplate(
        String code,
        String name,
        String description,
        String actorUserId,
        Instant now
    ) {
        if (findTemplateByCode(code).isPresent()) {
            throw new DomainException(ErrorCode.DUPLICATE_TEMPLATE_CODE, "Template code already exists");
        }
        String templateId = UUID.randomUUID().toString();
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
                ) VALUES (?, ?, ?, ?, TRUE, ?, ?)
                """,
            templateId,
            code,
            name,
            description,
            actorUserId,
            Timestamp.from(now)
        );
        return findTemplateByCode(code)
            .orElseThrow(() -> new DomainException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found after create"));
    }

    @Override
    public PermissionTemplateView updateTemplateMeta(
        String code,
        String name,
        String description,
        Boolean enabled,
        Instant now
    ) {
        PermissionTemplateView current = findTemplateByCode(code)
            .orElseThrow(() -> new DomainException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found"));
        jdbcTemplate.update(
            """
                UPDATE permission_templates
                SET name = COALESCE(?, name),
                    description = COALESCE(?, description),
                    enabled = COALESCE(?, enabled),
                    updated_at = ?
                WHERE code = ?
                """,
            name,
            description,
            enabled,
            Timestamp.from(now),
            code
        );
        return findTemplateByCode(code)
            .orElseThrow(() -> new DomainException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found after update"));
    }

    @Override
    public Set<String> replaceTemplatePermissions(String templateCode, Set<String> permissionCodes) {
        String templateId = getTemplateId(templateCode);
        ensurePermissionCodesExist(permissionCodes);
        jdbcTemplate.update("DELETE FROM template_permissions WHERE template_id = ?", templateId);
        for (String permissionCode : permissionCodes) {
            jdbcTemplate.update(
                """
                    INSERT INTO template_permissions (template_id, permission_id)
                    SELECT ?, p.permission_id
                    FROM permissions p
                    WHERE p.code = ?
                      AND p.enabled = TRUE
                    """,
                templateId,
                permissionCode
            );
        }
        return findPermissionCodesByTemplateId(templateId);
    }

    @Override
    public Set<String> addTemplatePermissions(String templateCode, Set<String> permissionCodes) {
        String templateId = getTemplateId(templateCode);
        ensurePermissionCodesExist(permissionCodes);
        for (String permissionCode : permissionCodes) {
            jdbcTemplate.update(
                """
                    INSERT INTO template_permissions (template_id, permission_id)
                    SELECT ?, p.permission_id
                    FROM permissions p
                    WHERE p.code = ?
                      AND p.enabled = TRUE
                      AND NOT EXISTS (
                          SELECT 1 FROM template_permissions tp
                          WHERE tp.template_id = ?
                            AND tp.permission_id = p.permission_id
                      )
                    """,
                templateId,
                permissionCode,
                templateId
            );
        }
        return findPermissionCodesByTemplateId(templateId);
    }

    @Override
    public Set<String> removeTemplatePermission(String templateCode, String permissionCode) {
        String templateId = getTemplateId(templateCode);
        jdbcTemplate.update(
            """
                DELETE FROM template_permissions
                WHERE template_id = ?
                  AND permission_id IN (
                      SELECT permission_id
                      FROM permissions
                      WHERE code = ?
                  )
                """,
            templateId,
            permissionCode
        );
        return findPermissionCodesByTemplateId(templateId);
    }

    @Override
    public TenantRoleTemplateBindingView bindTemplateToTenantRole(
        String tenantId,
        String roleCode,
        String templateCode,
        String actorUserId,
        Instant now
    ) {
        ensureTenantExists(tenantId);
        String templateId = getTemplateId(templateCode);
        TenantRoleTemplateBindingView existing = findBinding(tenantId, roleCode, templateCode).orElse(null);
        if (existing == null) {
            jdbcTemplate.update(
                """
                    INSERT INTO tenant_role_template_bindings (
                        binding_id,
                        tenant_id,
                        role_code,
                        template_id,
                        enabled,
                        created_by_user_id,
                        created_at
                    ) VALUES (?, ?, ?, ?, TRUE, ?, ?)
                    """,
                UUID.randomUUID().toString(),
                tenantId,
                roleCode,
                templateId,
                actorUserId,
                Timestamp.from(now)
            );
        } else {
            jdbcTemplate.update(
                """
                    UPDATE tenant_role_template_bindings
                    SET enabled = TRUE,
                        updated_at = ?
                    WHERE tenant_id = ?
                      AND role_code = ?
                      AND template_id = ?
                    """,
                Timestamp.from(now),
                tenantId,
                roleCode,
                templateId
            );
        }
        return findBinding(tenantId, roleCode, templateCode)
            .orElseThrow(() -> new DomainException(ErrorCode.TENANT_ROLE_TEMPLATE_BINDING_NOT_FOUND, "Binding not found after create"));
    }

    @Override
    public List<TenantRoleTemplateBindingView> findTenantRoleTemplateBindings(String tenantId) {
        ensureTenantExists(tenantId);
        return jdbcTemplate.query(
            """
                SELECT trtb.binding_id, trtb.tenant_id, trtb.role_code, t.code AS template_code, trtb.enabled
                FROM tenant_role_template_bindings trtb
                JOIN permission_templates t ON t.template_id = trtb.template_id
                WHERE trtb.tenant_id = ?
                ORDER BY trtb.role_code, t.code
                """,
            (rs, rowNum) -> mapBinding(rs),
            tenantId
        );
    }

    @Override
    public void disableTenantRoleTemplateBinding(String tenantId, String roleCode, String templateCode, Instant now) {
        ensureTenantExists(tenantId);
        String templateId = getTemplateId(templateCode);
        int updated = jdbcTemplate.update(
            """
                UPDATE tenant_role_template_bindings
                SET enabled = FALSE,
                    updated_at = ?
                WHERE tenant_id = ?
                  AND role_code = ?
                  AND template_id = ?
                """,
            Timestamp.from(now),
            tenantId,
            roleCode,
            templateId
        );
        if (updated == 0) {
            throw new DomainException(ErrorCode.TENANT_ROLE_TEMPLATE_BINDING_NOT_FOUND, "Binding not found");
        }
    }

    private Optional<TenantRoleTemplateBindingView> findBinding(String tenantId, String roleCode, String templateCode) {
        List<TenantRoleTemplateBindingView> rows = jdbcTemplate.query(
            """
                SELECT trtb.binding_id, trtb.tenant_id, trtb.role_code, t.code AS template_code, trtb.enabled
                FROM tenant_role_template_bindings trtb
                JOIN permission_templates t ON t.template_id = trtb.template_id
                WHERE trtb.tenant_id = ?
                  AND trtb.role_code = ?
                  AND t.code = ?
                """,
            (rs, rowNum) -> mapBinding(rs),
            tenantId,
            roleCode,
            templateCode
        );
        return rows.stream().findFirst();
    }

    private String getTemplateId(String templateCode) {
        return findTemplateByCode(templateCode)
            .map(PermissionTemplateView::templateId)
            .orElseThrow(() -> new DomainException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found"));
    }

    private Set<String> findPermissionCodesByTemplateId(String templateId) {
        return new LinkedHashSet<>(jdbcTemplate.query(
            """
                SELECT p.code
                FROM template_permissions tp
                JOIN permissions p ON p.permission_id = tp.permission_id
                WHERE tp.template_id = ?
                  AND p.enabled = TRUE
                ORDER BY p.code
                """,
            (rs, rowNum) -> rs.getString("code"),
            templateId
        ));
    }

    private void ensurePermissionCodesExist(Set<String> permissionCodes) {
        for (String permissionCode : permissionCodes) {
            if (!permissionCatalog.isKnown(permissionCode)) {
                throw new DomainException(ErrorCode.PERMISSION_NOT_FOUND, "Permission not found: " + permissionCode);
            }
        }
    }

    private void ensureTenantExists(String tenantId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM tenants WHERE tenant_id = ?",
            Integer.class,
            tenantId
        );
        if (count == null || count == 0) {
            throw new DomainException(ErrorCode.TENANT_NOT_FOUND, "Tenant not found");
        }
    }

    private PermissionTemplateView mapTemplate(ResultSet rs, Set<String> permissionCodes) throws SQLException {
        return new PermissionTemplateView(
            rs.getString("template_id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getBoolean("enabled"),
            permissionCodes.stream().toList()
        );
    }

    private TenantRoleTemplateBindingView mapBinding(ResultSet rs) throws SQLException {
        return new TenantRoleTemplateBindingView(
            rs.getString("binding_id"),
            rs.getString("tenant_id"),
            rs.getString("role_code"),
            rs.getString("template_code"),
            rs.getBoolean("enabled")
        );
    }
}

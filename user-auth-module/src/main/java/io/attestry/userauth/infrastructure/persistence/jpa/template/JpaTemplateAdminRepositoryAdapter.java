package io.attestry.userauth.infrastructure.persistence.jpa.template;

import io.attestry.userauth.application.port.template.PermissionCatalogPort;
import io.attestry.userauth.application.port.template.PermissionTemplatePort;
import io.attestry.userauth.application.port.template.TenantRoleTemplateBindingPort;
import io.attestry.userauth.infrastructure.persistence.jpa.membership.MembershipEffectivePermissionProjectionRefresher;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaTemplateAdminRepositoryAdapter
        implements PermissionTemplatePort, PermissionCatalogPort, TenantRoleTemplateBindingPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final MembershipEffectivePermissionProjectionRefresher permissionProjectionRefresher;

    @Override
    public Optional<PermissionTemplateView> findTemplateByCode(String templateCode) {
        List<PermissionTemplateView> rows = jdbcTemplate.getJdbcOperations().query(
            """
                SELECT template_id, tenant_id, code, name, description, enabled
                FROM permission_templates
                WHERE code = ?
                  AND tenant_id IS NULL
                """,
            (rs, rowNum) -> mapTemplate(rs, findPermissionCodesByTemplateId(rs.getString("template_id"))),
            templateCode
        );
        return rows.stream().findFirst();
    }

    @Override
    public Optional<PermissionTemplateView> findTemplateByCodeAndTenantId(String templateCode, String tenantId) {
        List<PermissionTemplateView> rows = jdbcTemplate.getJdbcOperations().query(
            """
                SELECT template_id, tenant_id, code, name, description, enabled
                FROM permission_templates
                WHERE code = ?
                  AND tenant_id = ?
                """,
            (rs, rowNum) -> mapTemplate(rs, findPermissionCodesByTemplateId(rs.getString("template_id"))),
            templateCode,
            tenantId
        );
        return rows.stream().findFirst();
    }

    @Override
    public Optional<PermissionTemplateView> findTemplateVisibleToTenant(String templateCode, String tenantId) {
        List<PermissionTemplateView> rows = jdbcTemplate.getJdbcOperations().query(
            """
                SELECT template_id, tenant_id, code, name, description, enabled
                FROM permission_templates
                WHERE code = ?
                  AND (tenant_id IS NULL OR tenant_id = ?)
                ORDER BY CASE WHEN tenant_id = ? THEN 0 ELSE 1 END
                LIMIT 1
                """,
            (rs, rowNum) -> mapTemplate(rs, findPermissionCodesByTemplateId(rs.getString("template_id"))),
            templateCode,
            tenantId,
            tenantId
        );
        return rows.stream().findFirst();
    }

    @Override
    public List<PermissionTemplateView> findAllTemplates() {
        return jdbcTemplate.getJdbcOperations().query(
            """
                SELECT template_id, tenant_id, code, name, description, enabled
                FROM permission_templates
                WHERE tenant_id IS NULL
                ORDER BY code
                """,
            (rs, rowNum) -> mapTemplate(rs, findPermissionCodesByTemplateId(rs.getString("template_id")))
        );
    }

    @Override
    public List<PermissionTemplateView> findTemplatesVisibleToTenant(String tenantId) {
        return jdbcTemplate.getJdbcOperations().query(
            """
                SELECT template_id, tenant_id, code, name, description, enabled
                FROM permission_templates
                WHERE tenant_id IS NULL OR tenant_id = ?
                ORDER BY CASE WHEN tenant_id = ? THEN 0 ELSE 1 END, code
                """,
            (rs, rowNum) -> mapTemplate(rs, findPermissionCodesByTemplateId(rs.getString("template_id"))),
            tenantId,
            tenantId
        );
    }

    @Override
    public PermissionTemplateView createTemplate(
        String code,
        String name,
        String description,
        String tenantId,
        String actorUserId,
        Instant now
    ) {
        Optional<PermissionTemplateView> existing = tenantId == null
            ? findTemplateByCode(code)
            : findTemplateByCodeAndTenantId(code, tenantId);
        if (existing.isPresent()) {
            throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_TEMPLATE_CODE, "Template code already exists");
        }
        String templateId = UUID.randomUUID().toString();
        jdbcTemplate.getJdbcOperations().update(
            """
                INSERT INTO permission_templates (
                    template_id,
                    tenant_id,
                    code,
                    name,
                    description,
                    enabled,
                    created_by_user_id,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, TRUE, ?, ?)
                """,
            templateId,
            tenantId,
            code,
            name,
            description,
            actorUserId,
            Timestamp.from(now)
        );
        return (tenantId == null ? findTemplateByCode(code) : findTemplateByCodeAndTenantId(code, tenantId))
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.TEMPLATE_NOT_FOUND, "Template not found after create"));
    }

    @Override
    public PermissionTemplateView updateTemplateMeta(
        String code,
        String tenantId,
        String name,
        String description,
        Boolean enabled,
        Instant now
    ) {
        PermissionTemplateView current = (tenantId == null
            ? findTemplateByCode(code)
            : findTemplateByCodeAndTenantId(code, tenantId))
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.TEMPLATE_NOT_FOUND, "Template not found"));
        jdbcTemplate.getJdbcOperations().update(
            """
                UPDATE permission_templates
                SET name = COALESCE(?, name),
                    description = COALESCE(?, description),
                    enabled = COALESCE(?, enabled),
                    updated_at = ?
                WHERE code = ?
                  AND (
                    (? IS NULL AND tenant_id IS NULL)
                    OR tenant_id = ?
                  )
                """,
            name,
            description,
            enabled,
            Timestamp.from(now),
            code,
            tenantId,
            tenantId
        );
        if (enabled != null) {
            permissionProjectionRefresher.refreshByTemplateId(current.templateId());
        }
        return (tenantId == null ? findTemplateByCode(code) : findTemplateByCodeAndTenantId(code, tenantId))
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.TEMPLATE_NOT_FOUND, "Template not found after update"));
    }

    @Override
    public PermissionView createPermission(
        String code,
        String name,
        String description,
        String resourceType,
        String action
    ) {
        Integer count = jdbcTemplate.getJdbcOperations().queryForObject(
            "SELECT COUNT(1) FROM permissions WHERE code = ?",
            Integer.class,
            code
        );
        if (count != null && count > 0) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_REQUEST, "Permission code already exists: " + code);
        }
        String permissionId = UUID.randomUUID().toString();
        jdbcTemplate.getJdbcOperations().update(
            """
                INSERT INTO permissions (
                    permission_id,
                    code,
                    name,
                    description,
                    resource_type,
                    action,
                    enabled
                ) VALUES (?, ?, ?, ?, ?, ?, TRUE)
                """,
            permissionId,
            code,
            name,
            description,
            resourceType,
            action
        );
        return findPermissionByCode(code)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.PERMISSION_NOT_FOUND, "Permission not found after create"));
    }

    @Override
    public List<PermissionView> findAllPermissions() {
        return jdbcTemplate.getJdbcOperations().query(
            """
                SELECT permission_id, code, name, description, resource_type, action, enabled
                FROM permissions
                ORDER BY code
                """,
            (rs, rowNum) -> mapPermission(rs)
        );
    }

    @Override
    public Set<String> replaceTemplatePermissions(String templateCode, String tenantId, Set<String> permissionCodes) {
        String templateId = getTemplateId(templateCode, tenantId);
        ensurePermissionCodesExist(permissionCodes);
        jdbcTemplate.getJdbcOperations().update("DELETE FROM template_permissions WHERE template_id = ?", templateId);
        for (String permissionCode : permissionCodes) {
            jdbcTemplate.getJdbcOperations().update(
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
        permissionProjectionRefresher.refreshByTemplateId(templateId);
        return findPermissionCodesByTemplateId(templateId);
    }

    @Override
    public Set<String> addTemplatePermissions(String templateCode, String tenantId, Set<String> permissionCodes) {
        String templateId = getTemplateId(templateCode, tenantId);
        ensurePermissionCodesExist(permissionCodes);
        for (String permissionCode : permissionCodes) {
            jdbcTemplate.getJdbcOperations().update(
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
        permissionProjectionRefresher.refreshByTemplateId(templateId);
        return findPermissionCodesByTemplateId(templateId);
    }

    @Override
    public Set<String> removeTemplatePermission(String templateCode, String tenantId, String permissionCode) {
        String templateId = getTemplateId(templateCode, tenantId);
        jdbcTemplate.getJdbcOperations().update(
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
        permissionProjectionRefresher.refreshByTemplateId(templateId);
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
        String templateId = getBindableTemplateId(tenantId, templateCode);
        TenantRoleTemplateBindingView existing = findBinding(tenantId, roleCode, templateCode).orElse(null);
        if (existing == null) {
            jdbcTemplate.getJdbcOperations().update(
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
            jdbcTemplate.getJdbcOperations().update(
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
        permissionProjectionRefresher.refreshTenant(tenantId);
        return findBinding(tenantId, roleCode, templateCode)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.TENANT_ROLE_TEMPLATE_BINDING_NOT_FOUND, "Binding not found after create"));
    }

    @Override
    public List<TenantRoleTemplateBindingView> findTenantRoleTemplateBindings(String tenantId) {
        ensureTenantExists(tenantId);
        return jdbcTemplate.getJdbcOperations().query(
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
        String templateId = getBindableTemplateId(tenantId, templateCode);
        int updated = jdbcTemplate.getJdbcOperations().update(
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
            throw new UserAuthDomainException(UserAuthErrorCode.TENANT_ROLE_TEMPLATE_BINDING_NOT_FOUND, "Binding not found");
        }
        permissionProjectionRefresher.refreshTenant(tenantId);
    }

    private Optional<TenantRoleTemplateBindingView> findBinding(String tenantId, String roleCode, String templateCode) {
        List<TenantRoleTemplateBindingView> rows = jdbcTemplate.getJdbcOperations().query(
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

    private String getTemplateId(String templateCode, String tenantId) {
        Optional<PermissionTemplateView> view = tenantId == null
            ? findTemplateByCode(templateCode)
            : findTemplateByCodeAndTenantId(templateCode, tenantId);
        return view
            .map(PermissionTemplateView::templateId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.TEMPLATE_NOT_FOUND, "Template not found"));
    }

    private String getBindableTemplateId(String tenantId, String templateCode) {
        return findTemplateVisibleToTenant(templateCode, tenantId)
            .map(PermissionTemplateView::templateId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.TEMPLATE_NOT_FOUND, "Template not found"));
    }

    private Set<String> findPermissionCodesByTemplateId(String templateId) {
        return new LinkedHashSet<>(jdbcTemplate.getJdbcOperations().query(
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
            Integer count = jdbcTemplate.getJdbcOperations().queryForObject(
                "SELECT COUNT(1) FROM permissions WHERE code = ? AND enabled = TRUE",
                Integer.class,
                permissionCode
            );
            if (count == null || count == 0) {
                throw new UserAuthDomainException(UserAuthErrorCode.PERMISSION_NOT_FOUND, "Permission not found: " + permissionCode);
            }
        }
    }

    private Optional<PermissionView> findPermissionByCode(String code) {
        List<PermissionView> rows = jdbcTemplate.getJdbcOperations().query(
            """
                SELECT permission_id, code, name, description, resource_type, action, enabled
                FROM permissions
                WHERE code = ?
                """,
            (rs, rowNum) -> mapPermission(rs),
            code
        );
        return rows.stream().findFirst();
    }

    private void ensureTenantExists(String tenantId) {
        Integer count = jdbcTemplate.getJdbcOperations().queryForObject(
            "SELECT COUNT(1) FROM tenants WHERE tenant_id = ?",
            Integer.class,
            tenantId
        );
        if (count == null || count == 0) {
            throw new UserAuthDomainException(UserAuthErrorCode.TENANT_NOT_FOUND, "Tenant not found");
        }
    }

    private PermissionTemplateView mapTemplate(ResultSet rs, Set<String> permissionCodes) throws SQLException {
        return new PermissionTemplateView(
            rs.getString("template_id"),
            rs.getString("tenant_id"),
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

    private PermissionView mapPermission(ResultSet rs) throws SQLException {
        return new PermissionView(
            rs.getString("permission_id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("resource_type"),
            rs.getString("action"),
            rs.getBoolean("enabled")
        );
    }
}

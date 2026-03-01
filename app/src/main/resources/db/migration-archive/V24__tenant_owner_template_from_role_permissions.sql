-- tenant owner template bootstrap from current role_permissions
-- source of truth for template seed: global TENANT_OWNER role-permission mapping

INSERT INTO permission_templates (
    template_id,
    code,
    name,
    description,
    enabled,
    created_by_user_id
)
SELECT
    'tpl-tenant-owner-core',
    'TEMPLATE_TENANT_OWNER_CORE',
    'Tenant Owner Core Template',
    'Default tenant owner operation permissions',
    TRUE,
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM permission_templates
    WHERE code = 'TEMPLATE_TENANT_OWNER_CORE'
);

-- synchronize template permissions from TENANT_OWNER role_permissions
DELETE FROM template_permissions
WHERE template_id = (
    SELECT template_id
    FROM permission_templates
    WHERE code = 'TEMPLATE_TENANT_OWNER_CORE'
);

INSERT INTO template_permissions (template_id, permission_id)
SELECT
    pt.template_id,
    rp.permission_id
FROM permission_templates pt
JOIN roles r
    ON r.tenant_id IS NULL
   AND r.code = 'TENANT_OWNER'
   AND r.enabled = TRUE
JOIN role_permissions rp
    ON rp.role_id = r.role_id
JOIN permissions p
    ON p.permission_id = rp.permission_id
   AND p.enabled = TRUE
WHERE pt.code = 'TEMPLATE_TENANT_OWNER_CORE'
  AND NOT EXISTS (
      SELECT 1
      FROM template_permissions tp
      WHERE tp.template_id = pt.template_id
        AND tp.permission_id = rp.permission_id
  );

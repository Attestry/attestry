-- Add tenant read-only scope and bind it to all tenant roles (OWNER/OPERATOR/STAFF)

-- 1) permission
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT
    'perm-tenant-read-only',
    'TENANT_READ_ONLY',
    'Tenant Read Only',
    'Read-only access for tenant scoped resources',
    'tenant',
    'read',
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions
    WHERE code = 'TENANT_READ_ONLY'
);

-- 2) default template
INSERT INTO permission_templates (
    template_id,
    code,
    name,
    description,
    enabled,
    created_by_user_id,
    created_at
)
SELECT
    'tpl-tenant-read-only',
    'TEMPLATE_TENANT_READ_ONLY',
    'Tenant Read Only Template',
    'Read-only permissions shared by tenant owner/operator/staff',
    TRUE,
    NULL,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM permission_templates
    WHERE code = 'TEMPLATE_TENANT_READ_ONLY'
);

INSERT INTO template_permissions (template_id, permission_id)
SELECT pt.template_id, p.permission_id
FROM permission_templates pt
JOIN permissions p ON p.code = 'TENANT_READ_ONLY'
WHERE pt.code = 'TEMPLATE_TENANT_READ_ONLY'
  AND NOT EXISTS (
      SELECT 1
      FROM template_permissions tp
      WHERE tp.template_id = pt.template_id
        AND tp.permission_id = p.permission_id
  );

-- 3) backfill bindings for all existing tenants and tenant role codes
WITH tenant_actor AS (
    SELECT m.tenant_id, MIN(m.user_id) AS actor_user_id
    FROM memberships m
    GROUP BY m.tenant_id
),
role_codes AS (
    SELECT 'TENANT_OWNER' AS role_code
    UNION ALL SELECT 'TENANT_OPERATOR'
    UNION ALL SELECT 'TENANT_STAFF'
)
INSERT INTO tenant_role_template_bindings (
    binding_id,
    tenant_id,
    role_code,
    template_id,
    enabled,
    created_by_user_id,
    created_at
)
SELECT
    'trtb-ro-' || SUBSTRING(MD5(t.tenant_id || ':' || rc.role_code) FROM 1 FOR 28),
    t.tenant_id,
    rc.role_code,
    pt.template_id,
    TRUE,
    ta.actor_user_id,
    CURRENT_TIMESTAMP
FROM tenants t
JOIN tenant_actor ta ON ta.tenant_id = t.tenant_id
JOIN role_codes rc ON TRUE
JOIN permission_templates pt ON pt.code = 'TEMPLATE_TENANT_READ_ONLY'
WHERE NOT EXISTS (
    SELECT 1
    FROM tenant_role_template_bindings trtb
    WHERE trtb.tenant_id = t.tenant_id
      AND trtb.role_code = rc.role_code
      AND trtb.template_id = pt.template_id
);

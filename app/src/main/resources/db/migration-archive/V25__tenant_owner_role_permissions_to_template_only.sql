-- move TENANT_OWNER effective permissions from role_permissions to template binding only
-- 1) backfill tenant-role-template binding for existing tenants
-- 2) remove TENANT_OWNER role_permissions mappings

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
    SUBSTRING(CONCAT('trtb-owner-', t.tenant_id), 1, 36),
    t.tenant_id,
    'TENANT_OWNER',
    pt.template_id,
    TRUE,
    owner_candidates.user_id,
    CURRENT_TIMESTAMP
FROM tenants t
JOIN permission_templates pt
    ON pt.code = 'TEMPLATE_TENANT_OWNER_CORE'
   AND pt.enabled = TRUE
JOIN (
    SELECT
        m.tenant_id,
        MIN(m.user_id) AS user_id
    FROM memberships m
    JOIN membership_role_assignments mra
        ON mra.membership_id = m.membership_id
    JOIN roles r
        ON r.role_id = mra.role_id
       AND r.tenant_id IS NULL
       AND r.code = 'TENANT_OWNER'
       AND r.enabled = TRUE
    GROUP BY m.tenant_id
) owner_candidates
    ON owner_candidates.tenant_id = t.tenant_id
WHERE NOT EXISTS (
    SELECT 1
    FROM tenant_role_template_bindings trtb
    WHERE trtb.tenant_id = t.tenant_id
      AND trtb.role_code = 'TENANT_OWNER'
      AND trtb.template_id = pt.template_id
);

DELETE FROM role_permissions
WHERE role_id = 'role-tenant-owner';

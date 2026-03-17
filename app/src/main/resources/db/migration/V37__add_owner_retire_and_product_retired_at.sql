ALTER TABLE product_assets
    ADD COLUMN IF NOT EXISTS retired_at TIMESTAMP;

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-owner-retire', 'OWNER_RETIRE', 'Owner Retire', 'Retire owned asset', 'owner', 'retire', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'OWNER_RETIRE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-owner-default', permission_id
FROM permissions
WHERE code = 'OWNER_RETIRE'
  AND enabled = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = 'role-owner-default'
        AND rp.permission_id = permissions.permission_id
  );

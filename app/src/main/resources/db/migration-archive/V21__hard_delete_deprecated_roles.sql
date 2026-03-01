-- hard delete deprecated global roles after V20 migration
-- safety: re-point any remaining deprecated assignments to TENANT_STAFF first

UPDATE membership_role_assignments
SET role_id = 'role-tenant-staff',
    assigned_at = CURRENT_TIMESTAMP
WHERE role_id IN (
    SELECT role_id
    FROM roles
    WHERE tenant_id IS NULL
      AND code IN (
        'TENANT_MEMBERSHIP_ADMIN',
        'TENANT_PASSPORT_ADMIN',
        'BRAND_ADMIN_BASE',
        'RETAIL_ADMIN_BASE',
        'BRAND_OPERATOR',
        'RETAIL_OPERATOR',
        'GROUP_STAFF'
      )
);

DELETE FROM role_permissions
WHERE role_id IN (
    SELECT role_id
    FROM roles
    WHERE tenant_id IS NULL
      AND code IN (
        'TENANT_MEMBERSHIP_ADMIN',
        'TENANT_PASSPORT_ADMIN',
        'BRAND_ADMIN_BASE',
        'RETAIL_ADMIN_BASE',
        'BRAND_OPERATOR',
        'RETAIL_OPERATOR',
        'GROUP_STAFF'
      )
);

DELETE FROM roles
WHERE tenant_id IS NULL
  AND code IN (
    'TENANT_MEMBERSHIP_ADMIN',
    'TENANT_PASSPORT_ADMIN',
    'BRAND_ADMIN_BASE',
    'RETAIL_ADMIN_BASE',
    'BRAND_OPERATOR',
    'RETAIL_OPERATOR',
    'GROUP_STAFF'
  );

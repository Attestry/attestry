-- RBAC v1 policy refactor
-- 1) permission 분해
-- 2) MEMBERSHIP_MANAGE / TENANT_ADMIN / FULFILLMENT_RELEASE deprecated
-- 3) role-permission 매핑 재구성

-- tenant operation permissions
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-group-create', 'TENANT_GROUP_CREATE', 'Tenant Group Create', 'Create tenant groups', 'tenant_group', 'create', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_GROUP_CREATE');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-group-suspend', 'TENANT_GROUP_SUSPEND', 'Tenant Group Suspend', 'Suspend tenant groups', 'tenant_group', 'suspend', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_GROUP_SUSPEND');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-group-resume', 'TENANT_GROUP_RESUME', 'Tenant Group Resume', 'Resume tenant groups', 'tenant_group', 'resume', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_GROUP_RESUME');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-invitation-create', 'TENANT_INVITATION_CREATE', 'Tenant Invitation Create', 'Create invitations in tenant', 'invitation', 'create', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_INVITATION_CREATE');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-invitation-revoke', 'TENANT_INVITATION_REVOKE', 'Tenant Invitation Revoke', 'Revoke invitations in tenant', 'invitation', 'revoke', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_INVITATION_REVOKE');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-invitation-view', 'TENANT_INVITATION_VIEW', 'Tenant Invitation View', 'View invitations in tenant', 'invitation', 'view', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_INVITATION_VIEW');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-membership-view', 'TENANT_MEMBERSHIP_VIEW', 'Tenant Membership View', 'View memberships in tenant', 'membership', 'view', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_MEMBERSHIP_VIEW');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-role-assign', 'TENANT_ROLE_ASSIGN', 'Tenant Role Assign', 'Assign/revoke roles in tenant', 'membership', 'role_assign', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_ROLE_ASSIGN');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-membership-enforce', 'TENANT_MEMBERSHIP_ENFORCE', 'Tenant Membership Enforce', 'Enforce membership status in tenant', 'membership', 'enforce', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_MEMBERSHIP_ENFORCE');

-- domain permission replacement
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-retail-release', 'RETAIL_RELEASE', 'Retail Release', 'Release operation by retail actor', 'retail', 'release', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'RETAIL_RELEASE');

-- deprecated permissions disable
UPDATE permissions
SET enabled = FALSE
WHERE code IN ('TENANT_ADMIN', 'MEMBERSHIP_MANAGE', 'FULFILLMENT_RELEASE');

-- tenant owner role (optional high-privilege role template)
INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-tenant-owner', NULL, 'TENANT_OWNER', 'Tenant Owner', 'High-privilege tenant operator role', 'ANY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'TENANT_OWNER');

-- platform role keeps platform-only permissions (idempotent re-assert)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-platform-super-admin', p.permission_id
FROM permissions p
WHERE p.code IN ('PLATFORM_ADMIN', 'TENANT_CREATE_APPROVE', 'TENANT_SUSPEND', 'GLOBAL_AUDIT_READ')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = 'role-platform-super-admin' AND rp.permission_id = p.permission_id
  );

-- tenant membership admin: assignment-only + invitation + membership view
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-tenant-membership-admin', p.permission_id
FROM permissions p
WHERE p.code IN (
    'TENANT_INVITATION_CREATE',
    'TENANT_INVITATION_REVOKE',
    'TENANT_INVITATION_VIEW',
    'TENANT_MEMBERSHIP_VIEW',
    'TENANT_ROLE_ASSIGN'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = 'role-tenant-membership-admin' AND rp.permission_id = p.permission_id
);

-- tenant owner: full tenant operation including enforce
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-tenant-owner', p.permission_id
FROM permissions p
WHERE p.code IN (
    'TENANT_GROUP_CREATE',
    'TENANT_GROUP_SUSPEND',
    'TENANT_GROUP_RESUME',
    'TENANT_INVITATION_CREATE',
    'TENANT_INVITATION_REVOKE',
    'TENANT_INVITATION_VIEW',
    'TENANT_MEMBERSHIP_VIEW',
    'TENANT_ROLE_ASSIGN',
    'TENANT_MEMBERSHIP_ENFORCE',
    'TENANT_AUDIT_READ'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = 'role-tenant-owner' AND rp.permission_id = p.permission_id
);

-- brand admin base: tenant operation(assignment-only) + brand execution
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-brand-admin-base', p.permission_id
FROM permissions p
WHERE p.code IN (
    'TENANT_INVITATION_CREATE',
    'TENANT_INVITATION_VIEW',
    'TENANT_MEMBERSHIP_VIEW',
    'TENANT_ROLE_ASSIGN',
    'TENANT_AUDIT_READ',
    'BRAND_MINT',
    'BRAND_VOID'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = 'role-brand-admin-base' AND rp.permission_id = p.permission_id
);

-- retail admin base: tenant operation(assignment-only) + retail execution
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-retail-admin-base', p.permission_id
FROM permissions p
WHERE p.code IN (
    'TENANT_GROUP_CREATE',
    'TENANT_INVITATION_CREATE',
    'TENANT_INVITATION_VIEW',
    'TENANT_MEMBERSHIP_VIEW',
    'TENANT_ROLE_ASSIGN',
    'TENANT_AUDIT_READ',
    'RETAIL_RELEASE',
    'RETAIL_TRANSFER_CREATE'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = 'role-retail-admin-base' AND rp.permission_id = p.permission_id
);

-- operators
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-retail-operator', p.permission_id
FROM permissions p
WHERE p.code IN ('RETAIL_RELEASE', 'RETAIL_TRANSFER_CREATE')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = 'role-retail-operator' AND rp.permission_id = p.permission_id
);

-- remove deprecated permission mappings from all roles
DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT permission_id
    FROM permissions
    WHERE code IN ('TENANT_ADMIN', 'MEMBERSHIP_MANAGE', 'FULFILLMENT_RELEASE')
);

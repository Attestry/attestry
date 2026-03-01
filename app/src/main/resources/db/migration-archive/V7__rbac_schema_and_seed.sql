CREATE TABLE IF NOT EXISTS permissions (
    permission_id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    resource_type VARCHAR(100),
    action VARCHAR(100),
    enabled BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS roles (
    role_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    group_type VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL,
    CONSTRAINT fk_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT uq_roles_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (role_id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (permission_id)
);

CREATE TABLE IF NOT EXISTS membership_role_assignments (
    assignment_id VARCHAR(36) PRIMARY KEY,
    membership_id VARCHAR(36) NOT NULL UNIQUE,
    role_id VARCHAR(36) NOT NULL,
    assigned_by_user_id VARCHAR(36),
    assigned_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_membership_role_assignments_membership FOREIGN KEY (membership_id) REFERENCES memberships (membership_id),
    CONSTRAINT fk_membership_role_assignments_role FOREIGN KEY (role_id) REFERENCES roles (role_id),
    CONSTRAINT fk_membership_role_assignments_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES user_accounts (user_id)
);

CREATE INDEX IF NOT EXISTS idx_roles_tenant ON roles (tenant_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission ON role_permissions (permission_id);
CREATE INDEX IF NOT EXISTS idx_membership_role_assignments_role ON membership_role_assignments (role_id);

-- permissions seed (enum Scope와 거의 동일)
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-platform-admin', 'PLATFORM_ADMIN', 'Platform Admin', 'Platform-wide administration', 'platform', 'admin', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PLATFORM_ADMIN');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-create-approve', 'TENANT_CREATE_APPROVE', 'Tenant Create Approve', 'Approve tenant onboarding', 'tenant', 'approve', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_CREATE_APPROVE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-suspend', 'TENANT_SUSPEND', 'Tenant Suspend', 'Suspend/unsuspend tenant', 'tenant', 'suspend', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_SUSPEND');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-global-audit-read', 'GLOBAL_AUDIT_READ', 'Global Audit Read', 'Read global audit logs', 'audit', 'read', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'GLOBAL_AUDIT_READ');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-admin', 'TENANT_ADMIN', 'Tenant Admin', 'Tenant operation admin permission', 'tenant', 'admin', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_ADMIN');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-brand-mint', 'BRAND_MINT', 'Brand Mint', 'Issue brand assets', 'brand', 'mint', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'BRAND_MINT');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-brand-void', 'BRAND_VOID', 'Brand Void', 'Void brand assets', 'brand', 'void', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'BRAND_VOID');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-fulfillment-release', 'FULFILLMENT_RELEASE', 'Fulfillment Release', 'Release for fulfillment', 'fulfillment', 'release', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'FULFILLMENT_RELEASE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-retail-transfer-create', 'RETAIL_TRANSFER_CREATE', 'Retail Transfer Create', 'Create retail transfer', 'retail', 'transfer_create', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'RETAIL_TRANSFER_CREATE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-passport-permission-grant', 'PASSPORT_PERMISSION_GRANT', 'Passport Permission Grant', 'Grant/revoke passport permissions', 'passport', 'grant', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PASSPORT_PERMISSION_GRANT');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-membership-manage', 'MEMBERSHIP_MANAGE', 'Membership Manage', 'Manage membership role assignment', 'membership', 'manage', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'MEMBERSHIP_MANAGE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-audit-read', 'TENANT_AUDIT_READ', 'Tenant Audit Read', 'Read tenant audit logs', 'audit', 'read', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_AUDIT_READ');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-owner-transfer-create', 'OWNER_TRANSFER_CREATE', 'Owner Transfer Create', 'Create owner transfer', 'owner', 'transfer_create', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'OWNER_TRANSFER_CREATE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-owner-transfer-accept', 'OWNER_TRANSFER_ACCEPT', 'Owner Transfer Accept', 'Accept owner transfer', 'owner', 'transfer_accept', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'OWNER_TRANSFER_ACCEPT');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-owner-risk-flag', 'OWNER_RISK_FLAG', 'Owner Risk Flag', 'Flag owner risk', 'owner', 'risk_flag', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'OWNER_RISK_FLAG');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-owner-risk-clear', 'OWNER_RISK_CLEAR', 'Owner Risk Clear', 'Clear owner risk', 'owner', 'risk_clear', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'OWNER_RISK_CLEAR');

-- roles seed (플랫폼/민감 권한 분리·축소)
INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-platform-super-admin', NULL, 'PLATFORM_SUPER_ADMIN', 'Platform Super Admin', 'Platform-only sensitive role', 'ANY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'PLATFORM_SUPER_ADMIN');
INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-brand-admin-base', NULL, 'BRAND_ADMIN_BASE', 'Brand Admin Base', 'Tenant operation role without sensitive grants', 'BRAND', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'BRAND_ADMIN_BASE');
INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-retail-admin-base', NULL, 'RETAIL_ADMIN_BASE', 'Retail Admin Base', 'Tenant operation role without sensitive grants', 'RETAIL', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'RETAIL_ADMIN_BASE');
INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-brand-operator', NULL, 'BRAND_OPERATOR', 'Brand Operator', 'Brand operator role', 'BRAND', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'BRAND_OPERATOR');
INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-retail-operator', NULL, 'RETAIL_OPERATOR', 'Retail Operator', 'Retail operator role', 'RETAIL', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'RETAIL_OPERATOR');
INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-group-staff', NULL, 'GROUP_STAFF', 'Group Staff', 'Least privilege group member role', 'ANY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'GROUP_STAFF');
INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-tenant-membership-admin', NULL, 'TENANT_MEMBERSHIP_ADMIN', 'Tenant Membership Admin', 'Sensitive membership assignment role', 'ANY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'TENANT_MEMBERSHIP_ADMIN');
INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-tenant-passport-admin', NULL, 'TENANT_PASSPORT_ADMIN', 'Tenant Passport Admin', 'Sensitive passport grant role', 'ANY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'TENANT_PASSPORT_ADMIN');
INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-owner-default', NULL, 'OWNER_DEFAULT', 'Owner Default', 'Owner default permissions', 'ANY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'OWNER_DEFAULT');

-- role permissions: platform role
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-platform-super-admin', 'perm-platform-admin'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-platform-super-admin' AND permission_id = 'perm-platform-admin');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-platform-super-admin', 'perm-tenant-create-approve'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-platform-super-admin' AND permission_id = 'perm-tenant-create-approve');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-platform-super-admin', 'perm-tenant-suspend'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-platform-super-admin' AND permission_id = 'perm-tenant-suspend');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-platform-super-admin', 'perm-global-audit-read'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-platform-super-admin' AND permission_id = 'perm-global-audit-read');

-- role permissions: tenant base roles (민감 권한 제외)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-brand-admin-base', 'perm-tenant-admin'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-brand-admin-base' AND permission_id = 'perm-tenant-admin');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-brand-admin-base', 'perm-tenant-audit-read'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-brand-admin-base' AND permission_id = 'perm-tenant-audit-read');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-brand-admin-base', 'perm-brand-mint'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-brand-admin-base' AND permission_id = 'perm-brand-mint');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-brand-admin-base', 'perm-brand-void'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-brand-admin-base' AND permission_id = 'perm-brand-void');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-brand-admin-base', 'perm-fulfillment-release'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-brand-admin-base' AND permission_id = 'perm-fulfillment-release');

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-retail-admin-base', 'perm-tenant-admin'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-retail-admin-base' AND permission_id = 'perm-tenant-admin');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-retail-admin-base', 'perm-tenant-audit-read'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-retail-admin-base' AND permission_id = 'perm-tenant-audit-read');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-retail-admin-base', 'perm-fulfillment-release'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-retail-admin-base' AND permission_id = 'perm-fulfillment-release');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-retail-admin-base', 'perm-retail-transfer-create'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-retail-admin-base' AND permission_id = 'perm-retail-transfer-create');

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-brand-operator', 'perm-brand-mint'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-brand-operator' AND permission_id = 'perm-brand-mint');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-brand-operator', 'perm-brand-void'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-brand-operator' AND permission_id = 'perm-brand-void');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-brand-operator', 'perm-fulfillment-release'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-brand-operator' AND permission_id = 'perm-fulfillment-release');

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-retail-operator', 'perm-fulfillment-release'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-retail-operator' AND permission_id = 'perm-fulfillment-release');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-retail-operator', 'perm-retail-transfer-create'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-retail-operator' AND permission_id = 'perm-retail-transfer-create');

-- 민감 권한 분리 역할
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-tenant-membership-admin', 'perm-membership-manage'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-tenant-membership-admin' AND permission_id = 'perm-membership-manage');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-tenant-passport-admin', 'perm-passport-permission-grant'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-tenant-passport-admin' AND permission_id = 'perm-passport-permission-grant');

-- owner default role
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-owner-default', 'perm-owner-transfer-create'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-owner-default' AND permission_id = 'perm-owner-transfer-create');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-owner-default', 'perm-owner-transfer-accept'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-owner-default' AND permission_id = 'perm-owner-transfer-accept');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-owner-default', 'perm-owner-risk-flag'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-owner-default' AND permission_id = 'perm-owner-risk-flag');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-owner-default', 'perm-owner-risk-clear'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-owner-default' AND permission_id = 'perm-owner-risk-clear');

-- 기존 memberships를 단일 role assignment로 백필
INSERT INTO membership_role_assignments (assignment_id, membership_id, role_id, assigned_by_user_id, assigned_at)
SELECT
    CONCAT('assign-', m.membership_id),
    m.membership_id,
    CASE
        WHEN m.role = 'ADMIN' AND m.group_type = 'BRAND' THEN 'role-brand-admin-base'
        WHEN m.role = 'ADMIN' AND m.group_type = 'RETAIL' THEN 'role-retail-admin-base'
        WHEN m.role = 'OPERATOR' AND m.group_type = 'BRAND' THEN 'role-brand-operator'
        WHEN m.role = 'OPERATOR' AND m.group_type = 'RETAIL' THEN 'role-retail-operator'
        ELSE 'role-group-staff'
    END,
    NULL,
    CURRENT_TIMESTAMP
FROM memberships m
WHERE NOT EXISTS (
    SELECT 1
    FROM membership_role_assignments a
    WHERE a.membership_id = m.membership_id
);

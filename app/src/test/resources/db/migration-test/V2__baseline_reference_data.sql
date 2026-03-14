-- Baseline reference data (canonical RBAC seed)
-- Keep schema in V1, and static/canonical reference data in this file.

-- permissions
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-platform-admin', 'PLATFORM_ADMIN', 'Platform Admin', 'Platform-wide administration', 'platform', 'admin', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PLATFORM_ADMIN');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-create-approve', 'TENANT_CREATE_APPROVE', 'Tenant Create Approve', 'Approve tenant onboarding', 'tenant', 'approve', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_CREATE_APPROVE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-suspend', 'TENANT_SUSPEND', 'Tenant Suspend', 'Suspend tenant', 'tenant', 'suspend', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_SUSPEND');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-global-audit-read', 'GLOBAL_AUDIT_READ', 'Global Audit Read', 'Read global audit logs', 'audit', 'read', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'GLOBAL_AUDIT_READ');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-group-suspend', 'TENANT_GROUP_SUSPEND', 'Tenant Group Suspend', 'Suspend tenant groups', 'tenant_group', 'suspend', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_GROUP_SUSPEND');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-group-resume', 'TENANT_GROUP_RESUME', 'Tenant Group Resume', 'Resume tenant groups', 'tenant_group', 'resume', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_GROUP_RESUME');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-invitation-create', 'TENANT_INVITATION_CREATE', 'Tenant Invitation Create', 'Create tenant invitation', 'invitation', 'create', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_INVITATION_CREATE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-invitation-revoke', 'TENANT_INVITATION_REVOKE', 'Tenant Invitation Revoke', 'Revoke tenant invitation', 'invitation', 'revoke', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_INVITATION_REVOKE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-invitation-view', 'TENANT_INVITATION_VIEW', 'Tenant Invitation View', 'View tenant invitation', 'invitation', 'view', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_INVITATION_VIEW');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-membership-view', 'TENANT_MEMBERSHIP_VIEW', 'Tenant Membership View', 'View memberships', 'membership', 'view', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_MEMBERSHIP_VIEW');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-read-only', 'TENANT_READ_ONLY', 'Tenant Read Only', 'Read-only access for tenant scoped resources', 'tenant', 'read', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_READ_ONLY');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-role-assign', 'TENANT_ROLE_ASSIGN', 'Tenant Role Assign', 'Assign/revoke membership roles', 'membership', 'assign', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_ROLE_ASSIGN');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-tenant-membership-enforce', 'TENANT_MEMBERSHIP_ENFORCE', 'Tenant Membership Enforce', 'Enforce membership status', 'membership', 'enforce', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TENANT_MEMBERSHIP_ENFORCE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-partner-link-create', 'PARTNER_LINK_CREATE', 'Partner Link Create', 'Create partner links', 'partner_link', 'create', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PARTNER_LINK_CREATE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-partner-link-read', 'PARTNER_LINK_READ', 'Partner Link Read', 'Read partner links', 'partner_link', 'read', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PARTNER_LINK_READ');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-partner-link-suspend', 'PARTNER_LINK_SUSPEND', 'Partner Link Suspend', 'Suspend partner links', 'partner_link', 'suspend', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PARTNER_LINK_SUSPEND');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-partner-link-resume', 'PARTNER_LINK_RESUME', 'Partner Link Resume', 'Resume partner links', 'partner_link', 'resume', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PARTNER_LINK_RESUME');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-partner-link-terminate', 'PARTNER_LINK_TERMINATE', 'Partner Link Terminate', 'Terminate partner links', 'partner_link', 'terminate', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PARTNER_LINK_TERMINATE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-partner-link-approve', 'PARTNER_LINK_APPROVE', 'Partner Link Approve', 'Approve/reject partner links', 'partner_link', 'approve', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PARTNER_LINK_APPROVE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-delegation-grant', 'DELEGATION_GRANT', 'Delegation Grant', 'Grant delegation', 'delegation', 'grant', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DELEGATION_GRANT');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-delegation-revoke', 'DELEGATION_REVOKE', 'Delegation Revoke', 'Revoke delegation', 'delegation', 'revoke', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DELEGATION_REVOKE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-delegation-read', 'DELEGATION_READ', 'Delegation Read', 'Read delegation', 'delegation', 'read', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DELEGATION_READ');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-brand-release', 'BRAND_RELEASE', 'Brand Release', 'Release brand assets', 'brand', 'release', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'BRAND_RELEASE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-brand-mint', 'BRAND_MINT', 'Brand Mint', 'Mint brand assets', 'brand', 'mint', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'BRAND_MINT');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-brand-void', 'BRAND_VOID', 'Brand Void', 'Void brand assets', 'brand', 'void', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'BRAND_VOID');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-retail-transfer-create', 'RETAIL_TRANSFER_CREATE', 'Retail Transfer Create', 'Create retail transfer', 'retail', 'transfer_create', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'RETAIL_TRANSFER_CREATE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-passport-permission-grant', 'PASSPORT_PERMISSION_GRANT', 'Passport Permission Grant', 'Grant passport permissions', 'passport', 'grant', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PASSPORT_PERMISSION_GRANT');
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
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-purchase-claim-approve', 'PURCHASE_CLAIM_APPROVE', 'Purchase Claim Approve', 'Approve purchase claims', 'claim', 'approve', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PURCHASE_CLAIM_APPROVE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-owner-service-create', 'OWNER_SERVICE_CREATE', 'Owner Service Create', 'Create service request as owner', 'owner', 'service_create', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'OWNER_SERVICE_CREATE');
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-service-complete', 'SERVICE_COMPLETE', 'Service Complete', 'Complete service request as provider', 'service', 'complete', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SERVICE_COMPLETE');

-- canonical global roles
INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-platform-super-admin', NULL, 'PLATFORM_SUPER_ADMIN', 'Platform Super Admin', 'Platform super admin role', 'ANY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'PLATFORM_SUPER_ADMIN');
INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-owner-default', NULL, 'OWNER_DEFAULT', 'Owner Default', 'Owner baseline role', 'ANY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'OWNER_DEFAULT');
INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-tenant-owner', NULL, 'TENANT_OWNER', 'Tenant Owner', 'Tenant owner role', 'ANY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'TENANT_OWNER');
INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-tenant-operator', NULL, 'TENANT_OPERATOR', 'Tenant Operator', 'Tenant operator role', 'ANY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'TENANT_OPERATOR');
INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-tenant-staff', NULL, 'TENANT_STAFF', 'Tenant Staff', 'Tenant staff role', 'ANY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'TENANT_STAFF');

-- canonical global role-permission mapping
DELETE FROM role_permissions
WHERE role_id IN ('role-platform-super-admin', 'role-owner-default');

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-platform-super-admin', permission_id
FROM permissions
WHERE code IN ('PLATFORM_ADMIN', 'TENANT_CREATE_APPROVE', 'TENANT_SUSPEND', 'GLOBAL_AUDIT_READ')
  AND enabled = TRUE;

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-owner-default', permission_id
FROM permissions
WHERE code IN ('OWNER_TRANSFER_CREATE', 'OWNER_TRANSFER_ACCEPT', 'OWNER_RISK_FLAG', 'OWNER_RISK_CLEAR')
  AND enabled = TRUE;

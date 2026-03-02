-- Squashed Flyway baseline generated on 2026-03-01
-- Source: V1..V25 merged in-order

-- >>> BEGIN V1__init_user_auth_schema.sql
CREATE TABLE IF NOT EXISTS tenants (
    tenant_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    region VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_accounts (
    user_id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    status VARCHAR(30) NOT NULL,
    verification_level VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS tenant_groups (
    group_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT fk_tenant_groups_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id)
);

CREATE TABLE IF NOT EXISTS memberships (
    membership_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    group_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    group_type VARCHAR(30) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    group_status VARCHAR(30) NOT NULL,
    tenant_status VARCHAR(30) NOT NULL,
    CONSTRAINT fk_memberships_user FOREIGN KEY (user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT fk_memberships_group FOREIGN KEY (group_id) REFERENCES tenant_groups (group_id),
    CONSTRAINT fk_memberships_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id)
);

CREATE TABLE IF NOT EXISTS organization_applications (
    application_id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    applicant_user_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36),
    org_name VARCHAR(255) NOT NULL,
    country VARCHAR(10) NOT NULL,
    biz_reg_no VARCHAR(100),
    evidence_group_id VARCHAR(36),
    status VARCHAR(30) NOT NULL,
    reviewed_by_admin_id VARCHAR(36),
    reviewed_at TIMESTAMP,
    reject_reason TEXT,
    CONSTRAINT fk_org_applicant_user FOREIGN KEY (applicant_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT fk_org_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id)
);

CREATE TABLE IF NOT EXISTS approval_requests (
    approval_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    target_id VARCHAR(36) NOT NULL,
    payload TEXT,
    status VARCHAR(30) NOT NULL,
    requested_by VARCHAR(36) NOT NULL,
    approved_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    decided_at TIMESTAMP,
    CONSTRAINT fk_approval_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_approval_requested_by FOREIGN KEY (requested_by) REFERENCES user_accounts (user_id),
    CONSTRAINT fk_approval_approved_by FOREIGN KEY (approved_by) REFERENCES user_accounts (user_id)
);


CREATE INDEX IF NOT EXISTS idx_memberships_user_id ON memberships (user_id);
CREATE INDEX IF NOT EXISTS idx_memberships_context ON memberships (user_id, tenant_id, group_id);
CREATE INDEX IF NOT EXISTS idx_groups_tenant_id ON tenant_groups (tenant_id);
CREATE INDEX IF NOT EXISTS idx_org_apps_tenant_id ON organization_applications (tenant_id);
CREATE INDEX IF NOT EXISTS idx_approvals_tenant_id ON approval_requests (tenant_id);

-- <<< END V1__init_user_auth_schema.sql

-- >>> BEGIN V2__add_invitations.sql
CREATE TABLE IF NOT EXISTS invitations (
    invitation_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    group_id VARCHAR(36) NOT NULL,
    invitee_email VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    invited_by VARCHAR(36) NOT NULL,
    invited_at TIMESTAMP NOT NULL,
    accepted_by VARCHAR(36),
    accepted_at TIMESTAMP,
    CONSTRAINT fk_invitations_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_invitations_group FOREIGN KEY (group_id) REFERENCES tenant_groups (group_id),
    CONSTRAINT fk_invitations_invited_by FOREIGN KEY (invited_by) REFERENCES user_accounts (user_id),
    CONSTRAINT fk_invitations_accepted_by FOREIGN KEY (accepted_by) REFERENCES user_accounts (user_id)
);

CREATE INDEX IF NOT EXISTS idx_invitations_tenant_id ON invitations (tenant_id);
CREATE INDEX IF NOT EXISTS idx_invitations_email_status ON invitations (invitee_email, status);

-- <<< END V2__add_invitations.sql

-- >>> BEGIN V3__onboarding_evidence_minio.sql
ALTER TABLE organization_applications
    ADD COLUMN IF NOT EXISTS evidence_bundle_id VARCHAR(36);

UPDATE organization_applications
SET evidence_bundle_id = evidence_group_id
WHERE evidence_bundle_id IS NULL
  AND evidence_group_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS onboarding_evidences (
    evidence_bundle_id VARCHAR(36) PRIMARY KEY,
    owner_user_id VARCHAR(36) NOT NULL,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_onboarding_evidences_owner FOREIGN KEY (owner_user_id) REFERENCES user_accounts (user_id)
);

CREATE INDEX IF NOT EXISTS idx_onboarding_evidences_owner ON onboarding_evidences (owner_user_id);

-- <<< END V3__onboarding_evidence_minio.sql

-- >>> BEGIN V4__onboarding_evidence_integrity.sql
ALTER TABLE organization_applications
    ALTER COLUMN evidence_bundle_id SET NOT NULL;

ALTER TABLE organization_applications
    ADD CONSTRAINT fk_org_apps_evidence_bundle
        FOREIGN KEY (evidence_bundle_id) REFERENCES onboarding_evidences (evidence_bundle_id);

CREATE INDEX IF NOT EXISTS idx_org_apps_evidence_bundle_id ON organization_applications (evidence_bundle_id);

ALTER TABLE onboarding_evidences
    ADD CONSTRAINT chk_onboarding_evidence_ready_state
        CHECK (
            (status = 'PENDING_UPLOAD' AND completed_at IS NULL)
                OR (status = 'READY' AND completed_at IS NOT NULL AND size_bytes IS NOT NULL AND size_bytes > 0)
        );

-- <<< END V4__onboarding_evidence_integrity.sql

-- >>> BEGIN V5__onboarding_evidence_bundle_file_split.sql
CREATE TABLE IF NOT EXISTS onboarding_evidence_bundles (
    evidence_bundle_id VARCHAR(36) PRIMARY KEY,
    owner_user_id VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_onboarding_evidence_bundles_owner FOREIGN KEY (owner_user_id) REFERENCES user_accounts (user_id)
);

CREATE TABLE IF NOT EXISTS onboarding_evidence_files (
    evidence_file_id VARCHAR(36) PRIMARY KEY,
    evidence_bundle_id VARCHAR(36) NOT NULL,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_onboarding_evidence_files_bundle FOREIGN KEY (evidence_bundle_id) REFERENCES onboarding_evidence_bundles (evidence_bundle_id)
);

INSERT INTO onboarding_evidence_bundles (evidence_bundle_id, owner_user_id, status, created_at, completed_at)
SELECT
    evidence_bundle_id,
    owner_user_id,
    CASE WHEN status = 'READY' THEN 'READY' ELSE 'COLLECTING' END,
    created_at,
    completed_at
FROM onboarding_evidences
WHERE NOT EXISTS (
    SELECT 1
    FROM onboarding_evidence_bundles b
    WHERE b.evidence_bundle_id = onboarding_evidences.evidence_bundle_id
);

INSERT INTO onboarding_evidence_files (
    evidence_file_id,
    evidence_bundle_id,
    object_key,
    original_file_name,
    content_type,
    size_bytes,
    status,
    created_at,
    completed_at
)
SELECT
    evidence_bundle_id,
    evidence_bundle_id,
    object_key,
    original_file_name,
    content_type,
    size_bytes,
    status,
    created_at,
    completed_at
FROM onboarding_evidences
WHERE NOT EXISTS (
    SELECT 1
    FROM onboarding_evidence_files f
    WHERE f.evidence_file_id = onboarding_evidences.evidence_bundle_id
);

ALTER TABLE organization_applications
    DROP CONSTRAINT IF EXISTS fk_org_apps_evidence_bundle;

ALTER TABLE organization_applications
    ADD CONSTRAINT fk_org_apps_evidence_bundle
        FOREIGN KEY (evidence_bundle_id) REFERENCES onboarding_evidence_bundles (evidence_bundle_id);

CREATE INDEX IF NOT EXISTS idx_onboarding_evidence_bundles_owner ON onboarding_evidence_bundles (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_onboarding_evidence_files_bundle ON onboarding_evidence_files (evidence_bundle_id);

ALTER TABLE onboarding_evidence_bundles
    ADD CONSTRAINT chk_onboarding_evidence_bundle_state
        CHECK (
            (status = 'COLLECTING' AND completed_at IS NULL)
                OR (status = 'READY' AND completed_at IS NOT NULL)
        );

ALTER TABLE onboarding_evidence_files
    ADD CONSTRAINT chk_onboarding_evidence_file_state
        CHECK (
            (status = 'PENDING_UPLOAD' AND completed_at IS NULL AND size_bytes IS NULL)
                OR (status = 'READY' AND completed_at IS NOT NULL AND size_bytes IS NOT NULL AND size_bytes > 0)
        );

-- <<< END V5__onboarding_evidence_bundle_file_split.sql

-- >>> BEGIN V6__role_assignment_audits.sql
CREATE TABLE IF NOT EXISTS role_assignment_audits (
    audit_id VARCHAR(36) PRIMARY KEY,
    actor_user_id VARCHAR(36) NOT NULL,
    actor_tenant_id VARCHAR(36),
    target_membership_id VARCHAR(36) NOT NULL,
    before_role VARCHAR(30),
    after_role VARCHAR(30),
    decision_source VARCHAR(30) NOT NULL,
    allowed BOOLEAN NOT NULL,
    reason_code VARCHAR(100),
    requested_at TIMESTAMP NOT NULL,
    decided_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_role_assignment_audits_actor_user FOREIGN KEY (actor_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT fk_role_assignment_audits_target_membership FOREIGN KEY (target_membership_id) REFERENCES memberships (membership_id)
);

CREATE INDEX IF NOT EXISTS idx_role_assignment_audits_actor_tenant ON role_assignment_audits (actor_tenant_id);
CREATE INDEX IF NOT EXISTS idx_role_assignment_audits_target_membership ON role_assignment_audits (target_membership_id);

-- <<< END V6__role_assignment_audits.sql

-- >>> BEGIN V7__rbac_schema_and_seed.sql
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

-- <<< END V7__rbac_schema_and_seed.sql

-- >>> BEGIN V8__organization_application_unique_constraints.sql
-- organization_applications uniqueness rules (cross-db compatible)
-- application-layer validation enforces:
-- BRAND: org_name / biz_reg_no global unique
-- RETAIL: org_name / biz_reg_no tenant unique
-- DB layer uses composite unique indexes for baseline protection.

CREATE UNIQUE INDEX IF NOT EXISTS uq_org_apps_type_tenant_org_name
    ON organization_applications (type, tenant_id, org_name);

CREATE UNIQUE INDEX IF NOT EXISTS uq_org_apps_type_tenant_biz_reg_no
    ON organization_applications (type, tenant_id, biz_reg_no);

-- <<< END V8__organization_application_unique_constraints.sql

-- >>> BEGIN V9__rbac_v1_policy_refactor.sql
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

-- <<< END V9__rbac_v1_policy_refactor.sql

-- >>> BEGIN V10__remove_fulfillment_release_permission.sql
-- Hard delete deprecated permission: FULFILLMENT_RELEASE
-- (already disabled in V9, now physically removed)

DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT permission_id
    FROM permissions
    WHERE code = 'FULFILLMENT_RELEASE'
);

DELETE FROM permissions
WHERE code = 'FULFILLMENT_RELEASE';

-- <<< END V10__remove_fulfillment_release_permission.sql

-- >>> BEGIN V11__workflow_partner_links.sql
CREATE TABLE IF NOT EXISTS partner_links (
    partner_link_id VARCHAR(36) PRIMARY KEY,
    brand_tenant_id VARCHAR(36) NOT NULL,
    partner_tenant_id VARCHAR(36) NOT NULL,
    partner_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_by_user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    approved_by_user_id VARCHAR(36),
    approved_at TIMESTAMP,
    terminated_at TIMESTAMP,
    reason VARCHAR(1000),
    CONSTRAINT fk_partner_links_brand_tenant FOREIGN KEY (brand_tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_partner_links_partner_tenant FOREIGN KEY (partner_tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_partner_links_created_by FOREIGN KEY (created_by_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT fk_partner_links_approved_by FOREIGN KEY (approved_by_user_id) REFERENCES user_accounts (user_id)
);

CREATE INDEX IF NOT EXISTS idx_partner_links_brand ON partner_links (brand_tenant_id);
CREATE INDEX IF NOT EXISTS idx_partner_links_partner ON partner_links (partner_tenant_id);
CREATE INDEX IF NOT EXISTS idx_partner_links_status ON partner_links (status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_partner_links_by_status
    ON partner_links (brand_tenant_id, partner_tenant_id, partner_type, status);

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-partner-link-create', 'PARTNER_LINK_CREATE', 'Partner Link Create', 'Create partner link request', 'partner_link', 'create', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PARTNER_LINK_CREATE');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-partner-link-read', 'PARTNER_LINK_READ', 'Partner Link Read', 'Read partner links', 'partner_link', 'read', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PARTNER_LINK_READ');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-partner-link-suspend', 'PARTNER_LINK_SUSPEND', 'Partner Link Suspend', 'Suspend partner link', 'partner_link', 'suspend', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PARTNER_LINK_SUSPEND');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-partner-link-resume', 'PARTNER_LINK_RESUME', 'Partner Link Resume', 'Resume partner link', 'partner_link', 'resume', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PARTNER_LINK_RESUME');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-partner-link-terminate', 'PARTNER_LINK_TERMINATE', 'Partner Link Terminate', 'Terminate partner link', 'partner_link', 'terminate', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PARTNER_LINK_TERMINATE');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-partner-link-approve', 'PARTNER_LINK_APPROVE', 'Partner Link Approve', 'Approve/reject partner link request', 'partner_link', 'approve', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PARTNER_LINK_APPROVE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-tenant-owner', p.permission_id
FROM permissions p
WHERE p.code IN (
    'PARTNER_LINK_CREATE',
    'PARTNER_LINK_READ',
    'PARTNER_LINK_SUSPEND',
    'PARTNER_LINK_RESUME',
    'PARTNER_LINK_TERMINATE'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = 'role-tenant-owner' AND rp.permission_id = p.permission_id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-platform-super-admin', p.permission_id
FROM permissions p
WHERE p.code IN ('PARTNER_LINK_APPROVE', 'PARTNER_LINK_READ')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = 'role-platform-super-admin' AND rp.permission_id = p.permission_id
);

-- <<< END V11__workflow_partner_links.sql

-- >>> BEGIN V12__workflow_delegations.sql
CREATE TABLE IF NOT EXISTS delegations (
    delegation_id VARCHAR(36) PRIMARY KEY,
    partner_link_id VARCHAR(36) NOT NULL,
    brand_tenant_id VARCHAR(36) NOT NULL,
    partner_tenant_id VARCHAR(36) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP,
    granted_by_user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    revoked_by_user_id VARCHAR(36),
    revoked_at TIMESTAMP,
    reason VARCHAR(1000),
    CONSTRAINT fk_delegations_partner_link FOREIGN KEY (partner_link_id) REFERENCES partner_links (partner_link_id),
    CONSTRAINT fk_delegations_brand_tenant FOREIGN KEY (brand_tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_delegations_partner_tenant FOREIGN KEY (partner_tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_delegations_granted_by FOREIGN KEY (granted_by_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT fk_delegations_revoked_by FOREIGN KEY (revoked_by_user_id) REFERENCES user_accounts (user_id)
);

CREATE INDEX IF NOT EXISTS idx_delegations_brand ON delegations (brand_tenant_id);
CREATE INDEX IF NOT EXISTS idx_delegations_partner ON delegations (partner_tenant_id);
CREATE INDEX IF NOT EXISTS idx_delegations_resource ON delegations (resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_delegations_status ON delegations (status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_delegations_active_scope
    ON delegations (brand_tenant_id, partner_tenant_id, resource_type, resource_id, permission_code, status);

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-delegation-grant', 'DELEGATION_GRANT', 'Delegation Grant', 'Grant delegation on resource', 'delegation', 'grant', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DELEGATION_GRANT');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-delegation-revoke', 'DELEGATION_REVOKE', 'Delegation Revoke', 'Revoke delegation on resource', 'delegation', 'revoke', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DELEGATION_REVOKE');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-delegation-read', 'DELEGATION_READ', 'Delegation Read', 'Read delegation records', 'delegation', 'read', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DELEGATION_READ');

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-tenant-owner', p.permission_id
FROM permissions p
WHERE p.code IN ('DELEGATION_GRANT', 'DELEGATION_REVOKE', 'DELEGATION_READ')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = 'role-tenant-owner' AND rp.permission_id = p.permission_id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-platform-super-admin', p.permission_id
FROM permissions p
WHERE p.code IN ('DELEGATION_READ')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = 'role-platform-super-admin' AND rp.permission_id = p.permission_id
);

-- <<< END V12__workflow_delegations.sql

-- >>> BEGIN V13__remove_tenant_group_create_permission.sql
-- Hard delete deprecated permission: TENANT_GROUP_CREATE
-- remove mappings first, then remove permission row

DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT permission_id
    FROM permissions
    WHERE code = 'TENANT_GROUP_CREATE'
);

DELETE FROM permissions
WHERE code = 'TENANT_GROUP_CREATE';

-- <<< END V13__remove_tenant_group_create_permission.sql

-- >>> BEGIN V14__drop_legacy_onboarding_evidences.sql
-- Legacy table cleanup: onboarding_evidences is no longer used
-- Evidence model is split into onboarding_evidence_bundles / onboarding_evidence_files

DROP TABLE IF EXISTS onboarding_evidences;

-- <<< END V14__drop_legacy_onboarding_evidences.sql

-- >>> BEGIN V15__drop_memberships_role_column.sql
-- v2 cleanup: membership.role is no longer part of authority model.
-- Authority is resolved from membership_role_assignments only.

ALTER TABLE memberships
    DROP COLUMN IF EXISTS role;

-- <<< END V15__drop_memberships_role_column.sql

-- >>> BEGIN V16__add_row_version_for_optimistic_lock.sql
-- Optimistic lock support (v1 hardening)
-- Apply to high-contention state-transition aggregates.

ALTER TABLE organization_applications
    ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE partner_links
    ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE delegations
    ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

-- <<< END V16__add_row_version_for_optimistic_lock.sql

-- >>> BEGIN V17__ledger_schema.sql
CREATE TABLE IF NOT EXISTS ledger_chain (
    passport_id VARCHAR(36) PRIMARY KEY,
    last_seq BIGINT,
    last_hash VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS ledger_entry (
    ledger_id VARCHAR(36) PRIMARY KEY,
    passport_id VARCHAR(36) NOT NULL,
    seq BIGINT NOT NULL,

    event_category VARCHAR(50) NOT NULL,
    event_action VARCHAR(50) NOT NULL,

    actor_role VARCHAR(50) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,

    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    payload_json TEXT NOT NULL,
    payload_canonical TEXT,
    data_hash VARCHAR(64) NOT NULL,
    prev_hash VARCHAR(64),
    entry_hash VARCHAR(64) NOT NULL,

    idempotency_key VARCHAR(255) UNIQUE,
    schema_version INT NOT NULL DEFAULT 1,

    CONSTRAINT fk_ledger_entry_chain FOREIGN KEY (passport_id) REFERENCES ledger_chain (passport_id),
    CONSTRAINT uq_ledger_entry_passport_seq UNIQUE (passport_id, seq)
);

CREATE INDEX IF NOT EXISTS idx_ledger_entry_passport_created_at
    ON ledger_entry (passport_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ledger_entry_event_category_action
    ON ledger_entry (event_category, event_action);

-- <<< END V17__ledger_schema.sql

-- >>> BEGIN V18__outbox_event.sql
CREATE TABLE IF NOT EXISTS outbox_event (
    event_id VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    idempotency_key VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_outbox_event_idempotency_key
    ON outbox_event (idempotency_key);

CREATE INDEX IF NOT EXISTS idx_outbox_event_status_created
    ON outbox_event (status, created_at);

-- <<< END V18__outbox_event.sql

-- >>> BEGIN V19__tenant_role_simplification_phase1.sql
-- tenant role simplification phase-1
-- 1) add tenant common roles: TENANT_OPERATOR, TENANT_STAFF
-- 2) keep legacy roles alive for compatibility (deprecation/disable in phase-3)

INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-tenant-operator', NULL, 'TENANT_OPERATOR', 'Tenant Operator', 'Tenant work operator role', 'ANY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'TENANT_OPERATOR');

INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-tenant-staff', NULL, 'TENANT_STAFF', 'Tenant Staff', 'Tenant baseline staff role', 'ANY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'TENANT_STAFF');

-- phase-1 keeps role-permission mapping intentionally minimal for TENANT_OPERATOR / TENANT_STAFF.
-- work permissions will be provided by template/override in later phases.

-- <<< END V19__tenant_role_simplification_phase1.sql

-- >>> BEGIN V20__tenant_role_simplification_phase2_and_overrides.sql
-- tenant role simplification phase-2
-- 1) create membership permission override table
-- 2) migrate deprecated role assignments to TENANT_STAFF
-- 3) disable deprecated global roles

CREATE TABLE IF NOT EXISTS membership_permission_overrides (
    override_id VARCHAR(36) PRIMARY KEY,
    membership_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    effect VARCHAR(10) NOT NULL,
    source VARCHAR(30) NOT NULL,
    reason VARCHAR(255),
    created_by_user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mpo_membership FOREIGN KEY (membership_id) REFERENCES memberships (membership_id),
    CONSTRAINT fk_mpo_permission FOREIGN KEY (permission_id) REFERENCES permissions (permission_id),
    CONSTRAINT fk_mpo_created_by FOREIGN KEY (created_by_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT uq_mpo_membership_permission UNIQUE (membership_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_mpo_membership ON membership_permission_overrides (membership_id);
CREATE INDEX IF NOT EXISTS idx_mpo_permission ON membership_permission_overrides (permission_id);

-- migrate deprecated role assignments to TENANT_STAFF (least privilege baseline)
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

-- disable deprecated roles (assignment/query path exclusion)
UPDATE roles
SET enabled = FALSE
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

-- <<< END V20__tenant_role_simplification_phase2_and_overrides.sql

-- >>> BEGIN V21__hard_delete_deprecated_roles.sql
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

-- <<< END V21__hard_delete_deprecated_roles.sql

-- >>> BEGIN V22__permission_templates.sql
-- template-rbac phase-1
-- platform-managed permission templates

CREATE TABLE IF NOT EXISTS permission_templates (
    template_id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_permission_templates_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES user_accounts (user_id)
);

CREATE TABLE IF NOT EXISTS template_permissions (
    template_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (template_id, permission_id),
    CONSTRAINT fk_template_permissions_template
        FOREIGN KEY (template_id) REFERENCES permission_templates (template_id),
    CONSTRAINT fk_template_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions (permission_id)
);

CREATE INDEX IF NOT EXISTS idx_template_permissions_template
    ON template_permissions (template_id);

CREATE INDEX IF NOT EXISTS idx_template_permissions_permission
    ON template_permissions (permission_id);

-- NOTE:
-- System default templates and template-permission mappings are now synchronized
-- by application code (RbacCatalogProjectionSync). Baseline keeps schema only.

-- <<< END V22__permission_templates.sql

-- >>> BEGIN V23__tenant_role_template_bindings.sql
-- template-rbac phase-2
-- tenant-specific role-template bindings

CREATE TABLE IF NOT EXISTS tenant_role_template_bindings (
    binding_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    role_code VARCHAR(100) NOT NULL,
    template_id VARCHAR(36) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_trtb_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_trtb_template
        FOREIGN KEY (template_id) REFERENCES permission_templates (template_id),
    CONSTRAINT fk_trtb_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT ck_trtb_role_code
        CHECK (role_code IN ('TENANT_OWNER', 'TENANT_OPERATOR', 'TENANT_STAFF')),
    CONSTRAINT uq_trtb_tenant_role_template
        UNIQUE (tenant_id, role_code, template_id)
);

CREATE INDEX IF NOT EXISTS idx_trtb_tenant_role_enabled
    ON tenant_role_template_bindings (tenant_id, role_code, enabled);

CREATE INDEX IF NOT EXISTS idx_trtb_template_enabled
    ON tenant_role_template_bindings (template_id, enabled);

-- <<< END V23__tenant_role_template_bindings.sql

-- >>> BEGIN V24__tenant_owner_template_from_role_permissions.sql
-- NOTE:
-- TEMPLATE_TENANT_OWNER_CORE is now managed by code catalog and projected at runtime.
-- Legacy SQL seed from role_permissions is removed.

-- <<< END V24__tenant_owner_template_from_role_permissions.sql

-- >>> BEGIN V25__tenant_owner_role_permissions_to_template_only.sql
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

-- <<< END V25__tenant_owner_role_permissions_to_template_only.sql

-- >>> BEGIN V26__rbac_seed_compaction.sql
-- Final compaction for baseline:
-- 1) keep only canonical global roles (3 tenant roles + platform + owner default)
-- 2) keep only canonical permission codes used by code catalog
-- 3) keep minimal global role_permissions (platform/owner), tenant permissions via templates

-- ensure canonical global roles exist
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

-- normalize role table to canonical global role set only
DELETE FROM membership_role_assignments
WHERE role_id IN (
    SELECT role_id
    FROM roles
    WHERE tenant_id IS NULL
      AND code NOT IN (
        'PLATFORM_SUPER_ADMIN',
        'OWNER_DEFAULT',
        'TENANT_OWNER',
        'TENANT_OPERATOR',
        'TENANT_STAFF'
      )
);

DELETE FROM role_permissions
WHERE role_id IN (
    SELECT role_id
    FROM roles
    WHERE tenant_id IS NULL
      AND code NOT IN (
        'PLATFORM_SUPER_ADMIN',
        'OWNER_DEFAULT',
        'TENANT_OWNER',
        'TENANT_OPERATOR',
        'TENANT_STAFF'
      )
);

DELETE FROM roles
WHERE tenant_id IS NULL
  AND code NOT IN (
    'PLATFORM_SUPER_ADMIN',
    'OWNER_DEFAULT',
    'TENANT_OWNER',
    'TENANT_OPERATOR',
    'TENANT_STAFF'
  );

-- drop non-catalog permissions (legacy/deprecated)
DELETE FROM membership_permission_overrides
WHERE permission_id IN (
    SELECT permission_id
    FROM permissions
    WHERE code NOT IN (
        'PLATFORM_ADMIN',
        'TENANT_CREATE_APPROVE',
        'TENANT_SUSPEND',
        'GLOBAL_AUDIT_READ',
        'TENANT_GROUP_SUSPEND',
        'TENANT_GROUP_RESUME',
        'TENANT_INVITATION_CREATE',
        'TENANT_INVITATION_REVOKE',
        'TENANT_INVITATION_VIEW',
        'TENANT_MEMBERSHIP_VIEW',
        'TENANT_ROLE_ASSIGN',
        'TENANT_MEMBERSHIP_ENFORCE',
        'PARTNER_LINK_CREATE',
        'PARTNER_LINK_READ',
        'PARTNER_LINK_SUSPEND',
        'PARTNER_LINK_RESUME',
        'PARTNER_LINK_TERMINATE',
        'PARTNER_LINK_APPROVE',
        'DELEGATION_GRANT',
        'DELEGATION_REVOKE',
        'DELEGATION_READ',
        'BRAND_MINT',
        'BRAND_VOID',
        'RETAIL_RELEASE',
        'RETAIL_TRANSFER_CREATE',
        'PASSPORT_PERMISSION_GRANT',
        'TENANT_AUDIT_READ',
        'OWNER_TRANSFER_CREATE',
        'OWNER_TRANSFER_ACCEPT',
        'OWNER_RISK_FLAG',
        'OWNER_RISK_CLEAR'
    )
);

DELETE FROM template_permissions
WHERE permission_id IN (
    SELECT permission_id
    FROM permissions
    WHERE code NOT IN (
        'PLATFORM_ADMIN',
        'TENANT_CREATE_APPROVE',
        'TENANT_SUSPEND',
        'GLOBAL_AUDIT_READ',
        'TENANT_GROUP_SUSPEND',
        'TENANT_GROUP_RESUME',
        'TENANT_INVITATION_CREATE',
        'TENANT_INVITATION_REVOKE',
        'TENANT_INVITATION_VIEW',
        'TENANT_MEMBERSHIP_VIEW',
        'TENANT_ROLE_ASSIGN',
        'TENANT_MEMBERSHIP_ENFORCE',
        'PARTNER_LINK_CREATE',
        'PARTNER_LINK_READ',
        'PARTNER_LINK_SUSPEND',
        'PARTNER_LINK_RESUME',
        'PARTNER_LINK_TERMINATE',
        'PARTNER_LINK_APPROVE',
        'DELEGATION_GRANT',
        'DELEGATION_REVOKE',
        'DELEGATION_READ',
        'BRAND_MINT',
        'BRAND_VOID',
        'RETAIL_RELEASE',
        'RETAIL_TRANSFER_CREATE',
        'PASSPORT_PERMISSION_GRANT',
        'TENANT_AUDIT_READ',
        'OWNER_TRANSFER_CREATE',
        'OWNER_TRANSFER_ACCEPT',
        'OWNER_RISK_FLAG',
        'OWNER_RISK_CLEAR'
    )
);

DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT permission_id
    FROM permissions
    WHERE code NOT IN (
        'PLATFORM_ADMIN',
        'TENANT_CREATE_APPROVE',
        'TENANT_SUSPEND',
        'GLOBAL_AUDIT_READ',
        'TENANT_GROUP_SUSPEND',
        'TENANT_GROUP_RESUME',
        'TENANT_INVITATION_CREATE',
        'TENANT_INVITATION_REVOKE',
        'TENANT_INVITATION_VIEW',
        'TENANT_MEMBERSHIP_VIEW',
        'TENANT_ROLE_ASSIGN',
        'TENANT_MEMBERSHIP_ENFORCE',
        'PARTNER_LINK_CREATE',
        'PARTNER_LINK_READ',
        'PARTNER_LINK_SUSPEND',
        'PARTNER_LINK_RESUME',
        'PARTNER_LINK_TERMINATE',
        'PARTNER_LINK_APPROVE',
        'DELEGATION_GRANT',
        'DELEGATION_REVOKE',
        'DELEGATION_READ',
        'BRAND_MINT',
        'BRAND_VOID',
        'RETAIL_RELEASE',
        'RETAIL_TRANSFER_CREATE',
        'PASSPORT_PERMISSION_GRANT',
        'TENANT_AUDIT_READ',
        'OWNER_TRANSFER_CREATE',
        'OWNER_TRANSFER_ACCEPT',
        'OWNER_RISK_FLAG',
        'OWNER_RISK_CLEAR'
    )
);

DELETE FROM permissions
WHERE code NOT IN (
    'PLATFORM_ADMIN',
    'TENANT_CREATE_APPROVE',
    'TENANT_SUSPEND',
    'GLOBAL_AUDIT_READ',
    'TENANT_GROUP_SUSPEND',
    'TENANT_GROUP_RESUME',
    'TENANT_INVITATION_CREATE',
    'TENANT_INVITATION_REVOKE',
    'TENANT_INVITATION_VIEW',
    'TENANT_MEMBERSHIP_VIEW',
    'TENANT_ROLE_ASSIGN',
    'TENANT_MEMBERSHIP_ENFORCE',
    'PARTNER_LINK_CREATE',
    'PARTNER_LINK_READ',
    'PARTNER_LINK_SUSPEND',
    'PARTNER_LINK_RESUME',
    'PARTNER_LINK_TERMINATE',
    'PARTNER_LINK_APPROVE',
    'DELEGATION_GRANT',
    'DELEGATION_REVOKE',
    'DELEGATION_READ',
    'BRAND_MINT',
    'BRAND_VOID',
    'RETAIL_RELEASE',
    'RETAIL_TRANSFER_CREATE',
    'PASSPORT_PERMISSION_GRANT',
    'TENANT_AUDIT_READ',
    'OWNER_TRANSFER_CREATE',
    'OWNER_TRANSFER_ACCEPT',
    'OWNER_RISK_FLAG',
    'OWNER_RISK_CLEAR'
);

-- normalize global role-permission mapping
DELETE FROM role_permissions
WHERE role_id IN ('role-platform-super-admin', 'role-owner-default', 'role-tenant-owner', 'role-tenant-operator', 'role-tenant-staff');

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-platform-super-admin', p.permission_id
FROM permissions p
WHERE p.code IN ('PLATFORM_ADMIN', 'TENANT_CREATE_APPROVE', 'TENANT_SUSPEND', 'GLOBAL_AUDIT_READ')
  AND p.enabled = TRUE;

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-owner-default', p.permission_id
FROM permissions p
WHERE p.code IN ('OWNER_TRANSFER_CREATE', 'OWNER_TRANSFER_ACCEPT', 'OWNER_RISK_FLAG', 'OWNER_RISK_CLEAR')
  AND p.enabled = TRUE;

-- TENANT_OWNER / TENANT_OPERATOR / TENANT_STAFF:
-- baseline role_permissions are intentionally empty.
-- tenant effective permissions are provided by tenant-role-template bindings and overrides.

-- <<< END V26__rbac_seed_compaction.sql

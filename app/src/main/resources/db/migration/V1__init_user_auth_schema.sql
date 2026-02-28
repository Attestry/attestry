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

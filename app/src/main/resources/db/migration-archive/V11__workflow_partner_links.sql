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

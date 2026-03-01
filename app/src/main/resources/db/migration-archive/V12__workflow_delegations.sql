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

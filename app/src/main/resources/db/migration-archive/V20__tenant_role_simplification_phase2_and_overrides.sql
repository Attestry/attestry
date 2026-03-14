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

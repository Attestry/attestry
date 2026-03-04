ALTER TABLE passport_permissions
    ADD COLUMN IF NOT EXISTS source_delegation_id VARCHAR(36);

ALTER TABLE passport_permissions
    ADD COLUMN IF NOT EXISTS source_tenant_id VARCHAR(36);

ALTER TABLE passport_permissions
    ADD COLUMN IF NOT EXISTS target_tenant_id VARCHAR(36);

ALTER TABLE passport_permissions
    ADD COLUMN IF NOT EXISTS resource_type VARCHAR(50);

ALTER TABLE passport_permissions
    ADD COLUMN IF NOT EXISTS resource_id VARCHAR(100);

ALTER TABLE passport_permissions
    ADD COLUMN IF NOT EXISTS permission_code VARCHAR(100);

ALTER TABLE passport_permissions
    ADD COLUMN IF NOT EXISTS last_synced_at TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uq_passport_permissions_source_delegation
    ON passport_permissions (source_delegation_id);

CREATE INDEX IF NOT EXISTS idx_passport_permissions_projection_lookup
    ON passport_permissions (passport_id, seller_group_id, status, expires_at);

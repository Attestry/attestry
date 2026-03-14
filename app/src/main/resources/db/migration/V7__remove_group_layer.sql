-- V6: Remove Group layer (ADR-001)
-- Tenant:Group was always 1:1 — Group is redundant. Move type to Tenant, drop all group_id columns.

-- Step 1: Add type column to tenants, populate from tenant_groups
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS type VARCHAR(30);

UPDATE tenants t
SET type = (
    SELECT g.type FROM tenant_groups g WHERE g.tenant_id = t.tenant_id LIMIT 1
)
WHERE t.type IS NULL;

-- Default any tenants without a group to INTERNAL
UPDATE tenants SET type = 'INTERNAL' WHERE type IS NULL;

ALTER TABLE tenants ALTER COLUMN type SET NOT NULL;

-- Step 2: Drop group_id from memberships
ALTER TABLE memberships DROP CONSTRAINT IF EXISTS fk_memberships_group;
DROP INDEX IF EXISTS idx_memberships_context;
ALTER TABLE memberships DROP COLUMN IF EXISTS group_id;
ALTER TABLE memberships DROP COLUMN IF EXISTS group_status;
CREATE INDEX IF NOT EXISTS idx_memberships_context ON memberships (user_id, tenant_id);

-- Step 3: Drop group_id from invitations
ALTER TABLE invitations DROP CONSTRAINT IF EXISTS fk_invitations_group;
ALTER TABLE invitations DROP COLUMN IF EXISTS group_id;

-- Step 4: Drop group_id from product_assets
ALTER TABLE product_assets DROP CONSTRAINT IF EXISTS fk_product_assets_group;
ALTER TABLE product_assets DROP CONSTRAINT IF EXISTS uq_product_assets_group_serial;
DROP INDEX IF EXISTS idx_product_assets_tenant_group;
ALTER TABLE product_assets DROP COLUMN IF EXISTS group_id;
-- Replace group+serial uniqueness with tenant+serial
ALTER TABLE product_assets ADD CONSTRAINT uq_product_assets_tenant_serial UNIQUE (tenant_id, serial_number);

-- Step 5: Drop group_id from product_passports
ALTER TABLE product_passports DROP CONSTRAINT IF EXISTS fk_product_passports_group;
DROP INDEX IF EXISTS idx_product_passports_tenant_group;
ALTER TABLE product_passports DROP COLUMN IF EXISTS group_id;
CREATE INDEX IF NOT EXISTS idx_product_passports_tenant ON product_passports (tenant_id);

-- Step 6: Rename seller_group_id → seller_tenant_id in passport_permissions
DROP INDEX IF EXISTS idx_passport_permissions_active_seller;
ALTER TABLE passport_permissions RENAME COLUMN seller_group_id TO seller_tenant_id;
CREATE INDEX IF NOT EXISTS idx_passport_permissions_active_seller
    ON passport_permissions (passport_id, seller_tenant_id, status, expires_at);

-- Step 7: Drop group_id from workflow_shipments, rename released_by_group_id
ALTER TABLE workflow_shipments DROP CONSTRAINT IF EXISTS fk_workflow_shipments_group;
DROP INDEX IF EXISTS idx_workflow_shipments_tenant_group;
ALTER TABLE workflow_shipments DROP COLUMN IF EXISTS group_id;
ALTER TABLE workflow_shipments RENAME COLUMN released_by_group_id TO released_by_tenant_id;
CREATE INDEX IF NOT EXISTS idx_workflow_shipments_tenant ON workflow_shipments (tenant_id);

-- Step 8: Drop group_id from workflow_shipment_evidence_groups
DROP INDEX IF EXISTS idx_workflow_shipment_evidence_groups_scope;
ALTER TABLE workflow_shipment_evidence_groups DROP COLUMN IF EXISTS group_id;
CREATE INDEX IF NOT EXISTS idx_workflow_shipment_evidence_groups_scope
    ON workflow_shipment_evidence_groups (tenant_id);

-- Step 9: Drop group_id from token_transfers
ALTER TABLE token_transfers DROP COLUMN IF EXISTS group_id;

-- Step 10: Drop group_id from purchase_claim_requests
DROP INDEX IF EXISTS idx_pcr_tenant_group_status;
ALTER TABLE purchase_claim_requests DROP COLUMN IF EXISTS group_id;
CREATE INDEX IF NOT EXISTS idx_pcr_tenant_status ON purchase_claim_requests (tenant_id, status);

-- Step 11: Drop provider_group_id from workflow_service_requests (provider_tenant_id already exists)
ALTER TABLE workflow_service_requests DROP COLUMN IF EXISTS provider_group_id;

-- Step 12: Drop tenant_groups table
DROP TABLE IF EXISTS tenant_groups;

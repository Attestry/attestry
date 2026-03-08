DROP INDEX IF EXISTS idx_pcr_tenant_status;
DROP INDEX IF EXISTS idx_pcr_tenant_group_status;

ALTER TABLE purchase_claim_requests
    DROP COLUMN IF EXISTS tenant_id;

CREATE INDEX IF NOT EXISTS idx_pcr_status_submitted_at
    ON purchase_claim_requests (status, submitted_at);

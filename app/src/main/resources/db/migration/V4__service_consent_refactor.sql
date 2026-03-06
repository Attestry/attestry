-- 1. submitted_by_user_id column (provider user who submitted)
ALTER TABLE workflow_service_requests
    ADD COLUMN IF NOT EXISTS submitted_by_user_id VARCHAR(36);

-- 2. consent upsert partial unique index
CREATE UNIQUE INDEX IF NOT EXISTS uq_pp_active_service_repair
    ON passport_permissions (passport_id, seller_group_id)
    WHERE scope = 'SERVICE_REPAIR' AND status = 'ACTIVE';

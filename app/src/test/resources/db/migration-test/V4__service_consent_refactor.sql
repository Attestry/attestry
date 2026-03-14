-- H2-compatible version: partial unique index not supported, skip it.

ALTER TABLE workflow_service_requests
    ADD COLUMN IF NOT EXISTS submitted_by_user_id VARCHAR(36);

ALTER TABLE workflow_service_requests
    DROP CONSTRAINT IF EXISTS chk_svc_status;

UPDATE workflow_service_requests
SET status = 'PENDING'
WHERE status = 'SUBMITTED';

ALTER TABLE workflow_service_requests
    ADD CONSTRAINT chk_svc_status
        CHECK (status IN ('PENDING','ACCEPTED','REJECTED','COMPLETED','CANCELLED'));

DROP INDEX IF EXISTS uq_svc_passport_submitted;

CREATE UNIQUE INDEX IF NOT EXISTS uq_svc_passport_open
    ON workflow_service_requests (passport_id)
    WHERE status IN ('PENDING', 'ACCEPTED');

ALTER TABLE workflow_service_requests
    DROP CONSTRAINT IF EXISTS chk_svc_status;

UPDATE workflow_service_requests
SET status = 'PENDING'
WHERE status = 'SUBMITTED';

ALTER TABLE workflow_service_requests
    ADD CONSTRAINT chk_svc_status
        CHECK (status IN ('PENDING','ACCEPTED','REJECTED','COMPLETED','CANCELLED'));

-- H2-compatible: partial unique index on status predicates is skipped.
DROP INDEX IF EXISTS uq_svc_passport_submitted;

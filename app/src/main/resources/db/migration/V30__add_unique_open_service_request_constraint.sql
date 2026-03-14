CREATE UNIQUE INDEX IF NOT EXISTS uq_workflow_service_requests_open_passport
    ON workflow_service_requests (passport_id)
    WHERE status IN ('PENDING', 'ACCEPTED');

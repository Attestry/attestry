ALTER TABLE workflow_service_requests
    ADD COLUMN IF NOT EXISTS service_request_method VARCHAR(32);

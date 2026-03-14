ALTER TABLE workflow_service_requests ADD COLUMN IF NOT EXISTS symptom_description TEXT;
ALTER TABLE workflow_service_requests ADD COLUMN IF NOT EXISTS requested_reservation_at TIMESTAMP;
ALTER TABLE workflow_service_requests ADD COLUMN IF NOT EXISTS contact_memo TEXT;
ALTER TABLE workflow_service_requests ADD COLUMN IF NOT EXISTS service_result_detail TEXT;
ALTER TABLE workflow_service_requests ADD COLUMN IF NOT EXISTS completion_memo TEXT;

ALTER TABLE workflow_service_requests
    ADD COLUMN IF NOT EXISTS symptom_description TEXT,
    ADD COLUMN IF NOT EXISTS requested_reservation_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS contact_memo TEXT,
    ADD COLUMN IF NOT EXISTS service_result_detail TEXT,
    ADD COLUMN IF NOT EXISTS completion_memo TEXT;

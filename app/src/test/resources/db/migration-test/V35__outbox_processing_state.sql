ALTER TABLE outbox_event
    ADD COLUMN IF NOT EXISTS processing_started_at TIMESTAMP;

ALTER TABLE outbox_event
    ADD COLUMN IF NOT EXISTS processing_owner VARCHAR(128);

CREATE INDEX IF NOT EXISTS idx_outbox_event_status_next_retry
    ON outbox_event (status, next_retry_at, created_at);

CREATE INDEX IF NOT EXISTS idx_outbox_event_processing_started_at
    ON outbox_event (processing_started_at);

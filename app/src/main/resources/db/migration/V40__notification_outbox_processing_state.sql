ALTER TABLE notification_outbox
    ADD COLUMN IF NOT EXISTS processing_started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS processing_owner VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_processing_timeout
    ON notification_outbox (processing_started_at)
    WHERE status = 'PROCESSING';

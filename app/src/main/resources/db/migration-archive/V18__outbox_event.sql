CREATE TABLE IF NOT EXISTS outbox_event (
    event_id VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    idempotency_key VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_outbox_event_idempotency_key
    ON outbox_event (idempotency_key);

CREATE INDEX IF NOT EXISTS idx_outbox_event_status_created
    ON outbox_event (status, created_at);

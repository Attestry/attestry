CREATE TABLE IF NOT EXISTS notification_outbox (
    id                VARCHAR(36)   NOT NULL,
    notification_type VARCHAR(50)   NOT NULL,
    recipient         VARCHAR(255)  NOT NULL,
    payload           TEXT          NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    retry_count       INT           NOT NULL DEFAULT 0,
    last_error        TEXT,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at           TIMESTAMP,
    next_retry_at     TIMESTAMP,
    CONSTRAINT pk_notification_outbox PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_status_retry
    ON notification_outbox (status, next_retry_at);

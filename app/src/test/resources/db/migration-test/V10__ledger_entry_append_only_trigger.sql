-- H2-compatible version: PL/pgSQL triggers not supported in H2.
-- Only apply the ALTER TABLE statement.

ALTER TABLE outbox_event ADD COLUMN next_retry_at TIMESTAMP;

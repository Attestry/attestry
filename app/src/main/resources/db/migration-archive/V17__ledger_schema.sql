CREATE TABLE IF NOT EXISTS ledger_chain (
    passport_id VARCHAR(36) PRIMARY KEY,
    last_seq BIGINT,
    last_hash VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS ledger_entry (
    ledger_id VARCHAR(36) PRIMARY KEY,
    passport_id VARCHAR(36) NOT NULL,
    seq BIGINT NOT NULL,

    event_category VARCHAR(50) NOT NULL,
    event_action VARCHAR(50) NOT NULL,

    actor_role VARCHAR(50) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,

    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    payload_json TEXT NOT NULL,
    payload_canonical TEXT,
    data_hash VARCHAR(64) NOT NULL,
    prev_hash VARCHAR(64),
    entry_hash VARCHAR(64) NOT NULL,

    idempotency_key VARCHAR(255) UNIQUE,
    schema_version INT NOT NULL DEFAULT 1,

    CONSTRAINT fk_ledger_entry_chain FOREIGN KEY (passport_id) REFERENCES ledger_chain (passport_id),
    CONSTRAINT uq_ledger_entry_passport_seq UNIQUE (passport_id, seq)
);

CREATE INDEX IF NOT EXISTS idx_ledger_entry_passport_created_at
    ON ledger_entry (passport_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ledger_entry_event_category_action
    ON ledger_entry (event_category, event_action);

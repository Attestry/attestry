CREATE SCHEMA IF NOT EXISTS ledger;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'ledger_chain'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'ledger' AND table_name = 'ledger_chain'
    ) THEN
        ALTER TABLE public.ledger_chain SET SCHEMA ledger;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'ledger_entry'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'ledger' AND table_name = 'ledger_entry'
    ) THEN
        ALTER TABLE public.ledger_entry SET SCHEMA ledger;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS ledger.ledger_chain (
    passport_id VARCHAR(36) PRIMARY KEY,
    last_seq BIGINT,
    last_hash VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS ledger.ledger_entry (
    ledger_id VARCHAR(36) PRIMARY KEY,
    passport_id VARCHAR(36) NOT NULL,
    seq BIGINT NOT NULL,
    event_category VARCHAR(50) NOT NULL,
    event_action VARCHAR(50) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payload_json jsonb NOT NULL,
    payload_canonical TEXT,
    data_hash VARCHAR(64) NOT NULL,
    prev_hash VARCHAR(64),
    entry_hash VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE,
    schema_version INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_ledger_entry_chain FOREIGN KEY (passport_id) REFERENCES ledger.ledger_chain (passport_id),
    CONSTRAINT uq_ledger_entry_passport_seq UNIQUE (passport_id, seq)
);

CREATE INDEX IF NOT EXISTS idx_ledger_entry_passport_created_at
    ON ledger.ledger_entry (passport_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ledger_entry_event_category_action
    ON ledger.ledger_entry (event_category, event_action);

CREATE OR REPLACE FUNCTION ledger.prevent_ledger_entry_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'ledger_entry is immutable: % operation is not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger t
        JOIN pg_class c ON c.oid = t.tgrelid
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE t.tgname = 'trg_ledger_entry_no_update'
          AND n.nspname = 'ledger'
          AND c.relname = 'ledger_entry'
    ) THEN
        CREATE TRIGGER trg_ledger_entry_no_update
            BEFORE UPDATE ON ledger.ledger_entry FOR EACH ROW
            EXECUTE FUNCTION ledger.prevent_ledger_entry_mutation();
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger t
        JOIN pg_class c ON c.oid = t.tgrelid
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE t.tgname = 'trg_ledger_entry_no_delete'
          AND n.nspname = 'ledger'
          AND c.relname = 'ledger_entry'
    ) THEN
        CREATE TRIGGER trg_ledger_entry_no_delete
            BEFORE DELETE ON ledger.ledger_entry FOR EACH ROW
            EXECUTE FUNCTION ledger.prevent_ledger_entry_mutation();
    END IF;
END $$;

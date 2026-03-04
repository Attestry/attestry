-- ledger_entry is an immutable, append-only audit log.
-- Block UPDATE and DELETE at the database level to prevent accidental mutation.

CREATE OR REPLACE FUNCTION prevent_ledger_entry_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'ledger_entry is immutable: % operation is not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_entry_no_update
    BEFORE UPDATE ON ledger_entry FOR EACH ROW
    EXECUTE FUNCTION prevent_ledger_entry_mutation();

CREATE TRIGGER trg_ledger_entry_no_delete
    BEFORE DELETE ON ledger_entry FOR EACH ROW
    EXECUTE FUNCTION prevent_ledger_entry_mutation();

-- Outbox retry backoff: add next_retry_at column for exponential backoff scheduling
ALTER TABLE outbox_event ADD COLUMN next_retry_at TIMESTAMP;

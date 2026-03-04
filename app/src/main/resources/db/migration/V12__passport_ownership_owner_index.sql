-- Index on owner_id for "my passports" query
CREATE INDEX IF NOT EXISTS idx_passport_ownership_owner_id
    ON passport_ownership (owner_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_token_transfers_pending_passport
    ON token_transfers (passport_id)
    WHERE status = 'PENDING';

CREATE TABLE token_transfers (
    transfer_id VARCHAR(36) PRIMARY KEY,
    passport_id VARCHAR(36) NOT NULL,
    transfer_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    accept_method VARCHAR(10) NOT NULL,
    from_owner_id VARCHAR(36),
    to_owner_id VARCHAR(36),
    tenant_id VARCHAR(36),
    group_id VARCHAR(36),
    qr_nonce VARCHAR(64),
    code_hash VARCHAR(64),
    code_salt VARCHAR(36),
    attempt_count INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id VARCHAR(36) NOT NULL,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancelled_by_user_id VARCHAR(36),
    CONSTRAINT fk_token_transfers_passport
        FOREIGN KEY (passport_id) REFERENCES product_passports (passport_id)
);

CREATE INDEX idx_token_transfers_status_expires ON token_transfers (status, expires_at);
CREATE INDEX idx_token_transfers_passport_status ON token_transfers (passport_id, status);

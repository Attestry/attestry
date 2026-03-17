CREATE TABLE IF NOT EXISTS signup_email_verifications (
    verification_id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP NULL,
    consumed_at TIMESTAMP NULL,
    resend_count INT NOT NULL DEFAULT 0,
    confirm_attempt_count INT NOT NULL DEFAULT 0,
    last_sent_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_signup_email_verifications_expires_at
    ON signup_email_verifications (expires_at);

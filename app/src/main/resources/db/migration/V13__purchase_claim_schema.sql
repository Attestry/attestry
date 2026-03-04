CREATE TABLE purchase_claim_requests (
    claim_id            VARCHAR(64) PRIMARY KEY,
    tenant_id           VARCHAR(64) NOT NULL,
    group_id            VARCHAR(64) NOT NULL,
    claimant_user_id    VARCHAR(64) NOT NULL,
    serial_number       VARCHAR(255) NOT NULL,
    model_name          VARCHAR(255) NOT NULL,
    evidence_group_id   VARCHAR(64) NOT NULL,
    note                TEXT,
    status              VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at        TIMESTAMP NOT NULL,
    reviewed_by_user_id VARCHAR(64),
    reviewed_at         TIMESTAMP,
    rejection_reason    TEXT,
    passport_id         VARCHAR(64),
    asset_id            VARCHAR(64),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pcr_claimant ON purchase_claim_requests (claimant_user_id);
CREATE INDEX idx_pcr_tenant_group_status ON purchase_claim_requests (tenant_id, group_id, status);

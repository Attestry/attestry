-- Product Assets: VOID, Risk 컬럼 추가
ALTER TABLE product_assets ADD COLUMN voided_at TIMESTAMP;
ALTER TABLE product_assets ADD COLUMN voided_reason VARCHAR(50);
ALTER TABLE product_assets ADD COLUMN voided_note TEXT;
ALTER TABLE product_assets ADD COLUMN stolen_at TIMESTAMP;
ALTER TABLE product_assets ADD COLUMN lost_at TIMESTAMP;
ALTER TABLE product_assets ADD COLUMN risk_reported_by VARCHAR(36);
ALTER TABLE product_assets ADD COLUMN police_report_no VARCHAR(100);

-- Passport Ownership (Projection)
CREATE TABLE IF NOT EXISTS passport_ownership (
    passport_id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_ledger_seq INT,
    CONSTRAINT fk_passport_ownership_passport
        FOREIGN KEY (passport_id) REFERENCES product_passports (passport_id)
);

-- Passport Permission
CREATE TABLE IF NOT EXISTS passport_permissions (
    permission_id VARCHAR(36) PRIMARY KEY,
    passport_id VARCHAR(36) NOT NULL,
    seller_group_id VARCHAR(36) NOT NULL,
    scope VARCHAR(50) NOT NULL DEFAULT 'RETAIL_SALE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_passport_permission_passport
        FOREIGN KEY (passport_id) REFERENCES product_passports (passport_id)
);

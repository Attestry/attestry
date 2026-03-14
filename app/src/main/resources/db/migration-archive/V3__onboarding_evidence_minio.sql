ALTER TABLE organization_applications
    ADD COLUMN IF NOT EXISTS evidence_bundle_id VARCHAR(36);

UPDATE organization_applications
SET evidence_bundle_id = evidence_group_id
WHERE evidence_bundle_id IS NULL
  AND evidence_group_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS onboarding_evidences (
    evidence_bundle_id VARCHAR(36) PRIMARY KEY,
    owner_user_id VARCHAR(36) NOT NULL,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_onboarding_evidences_owner FOREIGN KEY (owner_user_id) REFERENCES user_accounts (user_id)
);

CREATE INDEX IF NOT EXISTS idx_onboarding_evidences_owner ON onboarding_evidences (owner_user_id);

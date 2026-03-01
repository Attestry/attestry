CREATE TABLE IF NOT EXISTS onboarding_evidence_bundles (
    evidence_bundle_id VARCHAR(36) PRIMARY KEY,
    owner_user_id VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_onboarding_evidence_bundles_owner FOREIGN KEY (owner_user_id) REFERENCES user_accounts (user_id)
);

CREATE TABLE IF NOT EXISTS onboarding_evidence_files (
    evidence_file_id VARCHAR(36) PRIMARY KEY,
    evidence_bundle_id VARCHAR(36) NOT NULL,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_onboarding_evidence_files_bundle FOREIGN KEY (evidence_bundle_id) REFERENCES onboarding_evidence_bundles (evidence_bundle_id)
);

INSERT INTO onboarding_evidence_bundles (evidence_bundle_id, owner_user_id, status, created_at, completed_at)
SELECT
    evidence_bundle_id,
    owner_user_id,
    CASE WHEN status = 'READY' THEN 'READY' ELSE 'COLLECTING' END,
    created_at,
    completed_at
FROM onboarding_evidences
WHERE NOT EXISTS (
    SELECT 1
    FROM onboarding_evidence_bundles b
    WHERE b.evidence_bundle_id = onboarding_evidences.evidence_bundle_id
);

INSERT INTO onboarding_evidence_files (
    evidence_file_id,
    evidence_bundle_id,
    object_key,
    original_file_name,
    content_type,
    size_bytes,
    status,
    created_at,
    completed_at
)
SELECT
    evidence_bundle_id,
    evidence_bundle_id,
    object_key,
    original_file_name,
    content_type,
    size_bytes,
    status,
    created_at,
    completed_at
FROM onboarding_evidences
WHERE NOT EXISTS (
    SELECT 1
    FROM onboarding_evidence_files f
    WHERE f.evidence_file_id = onboarding_evidences.evidence_bundle_id
);

ALTER TABLE organization_applications
    DROP CONSTRAINT IF EXISTS fk_org_apps_evidence_bundle;

ALTER TABLE organization_applications
    ADD CONSTRAINT fk_org_apps_evidence_bundle
        FOREIGN KEY (evidence_bundle_id) REFERENCES onboarding_evidence_bundles (evidence_bundle_id);

CREATE INDEX IF NOT EXISTS idx_onboarding_evidence_bundles_owner ON onboarding_evidence_bundles (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_onboarding_evidence_files_bundle ON onboarding_evidence_files (evidence_bundle_id);

ALTER TABLE onboarding_evidence_bundles
    ADD CONSTRAINT chk_onboarding_evidence_bundle_state
        CHECK (
            (status = 'COLLECTING' AND completed_at IS NULL)
                OR (status = 'READY' AND completed_at IS NOT NULL)
        );

ALTER TABLE onboarding_evidence_files
    ADD CONSTRAINT chk_onboarding_evidence_file_state
        CHECK (
            (status = 'PENDING_UPLOAD' AND completed_at IS NULL AND size_bytes IS NULL)
                OR (status = 'READY' AND completed_at IS NOT NULL AND size_bytes IS NOT NULL AND size_bytes > 0)
        );

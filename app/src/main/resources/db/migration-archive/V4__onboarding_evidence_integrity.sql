ALTER TABLE organization_applications
    ALTER COLUMN evidence_bundle_id SET NOT NULL;

ALTER TABLE organization_applications
    ADD CONSTRAINT fk_org_apps_evidence_bundle
        FOREIGN KEY (evidence_bundle_id) REFERENCES onboarding_evidences (evidence_bundle_id);

CREATE INDEX IF NOT EXISTS idx_org_apps_evidence_bundle_id ON organization_applications (evidence_bundle_id);

ALTER TABLE onboarding_evidences
    ADD CONSTRAINT chk_onboarding_evidence_ready_state
        CHECK (
            (status = 'PENDING_UPLOAD' AND completed_at IS NULL)
                OR (status = 'READY' AND completed_at IS NOT NULL AND size_bytes IS NOT NULL AND size_bytes > 0)
        );

ALTER TABLE purchase_claim_requests
    ADD COLUMN IF NOT EXISTS submitter_profile_type VARCHAR(32) NOT NULL DEFAULT 'OWNER';


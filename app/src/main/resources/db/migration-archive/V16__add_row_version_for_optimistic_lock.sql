-- Optimistic lock support (v1 hardening)
-- Apply to high-contention state-transition aggregates.

ALTER TABLE organization_applications
    ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE partner_links
    ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE delegations
    ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

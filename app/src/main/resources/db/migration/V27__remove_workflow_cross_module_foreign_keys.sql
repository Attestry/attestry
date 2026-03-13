-- Remove workflow foreign keys that couple workflow tables to external module-owned tables.
-- Keep workflow-internal referential constraints intact.

ALTER TABLE partner_links
    DROP CONSTRAINT IF EXISTS fk_partner_links_brand_tenant,
    DROP CONSTRAINT IF EXISTS fk_partner_links_partner_tenant,
    DROP CONSTRAINT IF EXISTS fk_partner_links_created_by,
    DROP CONSTRAINT IF EXISTS fk_partner_links_approved_by;

ALTER TABLE delegations
    DROP CONSTRAINT IF EXISTS fk_delegations_brand_tenant,
    DROP CONSTRAINT IF EXISTS fk_delegations_partner_tenant,
    DROP CONSTRAINT IF EXISTS fk_delegations_granted_by,
    DROP CONSTRAINT IF EXISTS fk_delegations_revoked_by;

ALTER TABLE workflow_shipments
    DROP CONSTRAINT IF EXISTS fk_workflow_shipments_tenant,
    DROP CONSTRAINT IF EXISTS fk_workflow_shipments_group,
    DROP CONSTRAINT IF EXISTS fk_workflow_shipments_passport,
    DROP CONSTRAINT IF EXISTS fk_workflow_shipments_released_user;

ALTER TABLE token_transfers
    DROP CONSTRAINT IF EXISTS fk_token_transfers_passport;

-- Remove workflow foreign keys that couple workflow tables to external module-owned tables.
-- Keep workflow-internal referential constraints intact.
-- H2 does not support multiple DROP CONSTRAINT in a single ALTER TABLE, so each is separate.

ALTER TABLE partner_links DROP CONSTRAINT IF EXISTS fk_partner_links_brand_tenant;
ALTER TABLE partner_links DROP CONSTRAINT IF EXISTS fk_partner_links_partner_tenant;
ALTER TABLE partner_links DROP CONSTRAINT IF EXISTS fk_partner_links_created_by;
ALTER TABLE partner_links DROP CONSTRAINT IF EXISTS fk_partner_links_approved_by;

ALTER TABLE delegations DROP CONSTRAINT IF EXISTS fk_delegations_brand_tenant;
ALTER TABLE delegations DROP CONSTRAINT IF EXISTS fk_delegations_partner_tenant;
ALTER TABLE delegations DROP CONSTRAINT IF EXISTS fk_delegations_granted_by;
ALTER TABLE delegations DROP CONSTRAINT IF EXISTS fk_delegations_revoked_by;

ALTER TABLE workflow_shipments DROP CONSTRAINT IF EXISTS fk_workflow_shipments_tenant;
ALTER TABLE workflow_shipments DROP CONSTRAINT IF EXISTS fk_workflow_shipments_group;
ALTER TABLE workflow_shipments DROP CONSTRAINT IF EXISTS fk_workflow_shipments_passport;
ALTER TABLE workflow_shipments DROP CONSTRAINT IF EXISTS fk_workflow_shipments_released_user;

ALTER TABLE token_transfers DROP CONSTRAINT IF EXISTS fk_token_transfers_passport;

-- Drop cross-module foreign keys after projection-based decoupling.
-- Keep intra-module foreign keys intact.

-- product -> user-auth
ALTER TABLE product_assets DROP CONSTRAINT IF EXISTS fk_product_assets_tenant;
ALTER TABLE product_assets DROP CONSTRAINT IF EXISTS fk_product_assets_group;
ALTER TABLE product_assets DROP CONSTRAINT IF EXISTS fk_product_assets_owner;
ALTER TABLE product_passports DROP CONSTRAINT IF EXISTS fk_product_passports_tenant;
ALTER TABLE product_passports DROP CONSTRAINT IF EXISTS fk_product_passports_group;

-- workflow -> user-auth
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
ALTER TABLE workflow_shipments DROP CONSTRAINT IF EXISTS fk_workflow_shipments_released_user;

-- workflow -> product
ALTER TABLE workflow_shipments DROP CONSTRAINT IF EXISTS fk_workflow_shipments_passport;
ALTER TABLE token_transfers DROP CONSTRAINT IF EXISTS fk_token_transfers_passport;

-- product-owned permission/ownership tables currently used across modules
ALTER TABLE passport_ownership DROP CONSTRAINT IF EXISTS fk_passport_ownership_passport;
ALTER TABLE passport_permissions DROP CONSTRAINT IF EXISTS fk_passport_permission_passport;

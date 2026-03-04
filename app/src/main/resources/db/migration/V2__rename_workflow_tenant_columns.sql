-- Rename workflow tenant columns from brand/partner naming to source/target naming.
-- Plain ALTER TABLE for H2 + PostgreSQL compatibility.

ALTER TABLE partner_links RENAME COLUMN brand_tenant_id TO source_tenant_id;
ALTER TABLE partner_links RENAME COLUMN partner_tenant_id TO target_tenant_id;

ALTER TABLE delegations RENAME COLUMN brand_tenant_id TO source_tenant_id;
ALTER TABLE delegations RENAME COLUMN partner_tenant_id TO target_tenant_id;

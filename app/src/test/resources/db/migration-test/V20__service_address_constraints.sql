ALTER TABLE organization_applications
    DROP CONSTRAINT IF EXISTS chk_org_apps_service_address_required;

ALTER TABLE organization_applications
    ADD CONSTRAINT chk_org_apps_service_address_required
    CHECK (type <> 'SERVICE' OR (address IS NOT NULL AND btrim(address) <> ''));

ALTER TABLE tenants
    DROP CONSTRAINT IF EXISTS chk_tenants_service_address_required;

ALTER TABLE tenants
    ADD CONSTRAINT chk_tenants_service_address_required
    CHECK (type <> 'SERVICE' OR (address IS NOT NULL AND btrim(address) <> ''));

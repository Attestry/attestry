-- organization_applications uniqueness rules (cross-db compatible)
-- application-layer validation enforces:
-- BRAND: org_name / biz_reg_no global unique
-- RETAIL: org_name / biz_reg_no tenant unique
-- DB layer uses composite unique indexes for baseline protection.

CREATE UNIQUE INDEX IF NOT EXISTS uq_org_apps_type_tenant_org_name
    ON organization_applications (type, tenant_id, org_name);

CREATE UNIQUE INDEX IF NOT EXISTS uq_org_apps_type_tenant_biz_reg_no
    ON organization_applications (type, tenant_id, biz_reg_no);

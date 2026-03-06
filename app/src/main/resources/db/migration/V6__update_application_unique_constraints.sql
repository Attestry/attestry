-- Drop old unique indexes
DROP INDEX IF EXISTS uq_org_apps_type_tenant_org_name;
DROP INDEX IF EXISTS uq_org_apps_type_tenant_biz_reg_no;

-- orgName uniqueness: type + tenant_id + country + org_name, excluding REJECTED
CREATE UNIQUE INDEX uq_org_apps_type_tenant_country_org_name
    ON organization_applications (type, tenant_id, country, org_name)
    WHERE status != 'REJECTED';

-- bizRegNo uniqueness: type + tenant_id + biz_reg_no, excluding REJECTED
CREATE UNIQUE INDEX uq_org_apps_type_tenant_biz_reg_no
    ON organization_applications (type, tenant_id, biz_reg_no)
    WHERE status != 'REJECTED';

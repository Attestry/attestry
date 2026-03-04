-- Tenant-scoped custom templates support
-- Global templates: tenant_id IS NULL
-- Tenant custom templates: tenant_id = target tenant

ALTER TABLE permission_templates
    ADD COLUMN tenant_id VARCHAR(36);

ALTER TABLE permission_templates
    ADD CONSTRAINT fk_permission_templates_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id);

CREATE INDEX IF NOT EXISTS idx_permission_templates_tenant
    ON permission_templates (tenant_id);

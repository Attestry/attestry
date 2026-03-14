-- template-rbac phase-2
-- tenant-specific role-template bindings

CREATE TABLE IF NOT EXISTS tenant_role_template_bindings (
    binding_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    role_code VARCHAR(100) NOT NULL,
    template_id VARCHAR(36) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_trtb_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_trtb_template
        FOREIGN KEY (template_id) REFERENCES permission_templates (template_id),
    CONSTRAINT fk_trtb_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT ck_trtb_role_code
        CHECK (role_code IN ('TENANT_OWNER', 'TENANT_OPERATOR', 'TENANT_STAFF')),
    CONSTRAINT uq_trtb_tenant_role_template
        UNIQUE (tenant_id, role_code, template_id)
);

CREATE INDEX IF NOT EXISTS idx_trtb_tenant_role_enabled
    ON tenant_role_template_bindings (tenant_id, role_code, enabled);

CREATE INDEX IF NOT EXISTS idx_trtb_template_enabled
    ON tenant_role_template_bindings (template_id, enabled);

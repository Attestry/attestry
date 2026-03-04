CREATE TABLE IF NOT EXISTS product_assets (
    asset_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    group_id VARCHAR(36) NOT NULL,
    serial_number VARCHAR(120) NOT NULL,
    model_id VARCHAR(120),
    model_name VARCHAR(255) NOT NULL,
    manufactured_at TIMESTAMP NOT NULL,
    production_batch VARCHAR(120),
    factory_code VARCHAR(120),
    component_root_hash VARCHAR(64),
    asset_state VARCHAR(30) NOT NULL,
    risk_flag VARCHAR(30) NOT NULL,
    ownership_user_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_assets_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_product_assets_group FOREIGN KEY (group_id) REFERENCES tenant_groups (group_id),
    CONSTRAINT fk_product_assets_owner FOREIGN KEY (ownership_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT uq_product_assets_group_serial UNIQUE (group_id, serial_number)
);

CREATE INDEX IF NOT EXISTS idx_product_assets_tenant_group
    ON product_assets (tenant_id, group_id);

CREATE TABLE IF NOT EXISTS product_passports (
    passport_id VARCHAR(36) PRIMARY KEY,
    asset_id VARCHAR(36) NOT NULL UNIQUE,
    tenant_id VARCHAR(36) NOT NULL,
    group_id VARCHAR(36) NOT NULL,
    qr_public_code VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_passports_asset FOREIGN KEY (asset_id) REFERENCES product_assets (asset_id),
    CONSTRAINT fk_product_passports_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_product_passports_group FOREIGN KEY (group_id) REFERENCES tenant_groups (group_id)
);

CREATE INDEX IF NOT EXISTS idx_product_passports_tenant_group
    ON product_passports (tenant_id, group_id);

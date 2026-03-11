CREATE TABLE IF NOT EXISTS workflow_passport_state_projection (
    passport_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    asset_id VARCHAR(36) NOT NULL,
    asset_state VARCHAR(30) NOT NULL,
    risk_flag VARCHAR(30) NOT NULL,
    current_owner_id VARCHAR(36),
    source_event_id VARCHAR(100) NOT NULL,
    source_event_version BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wpsp_source_event_id UNIQUE (source_event_id),
    CONSTRAINT chk_wpsp_asset_state CHECK (asset_state IN ('ACTIVE', 'VOIDED', 'STOLEN', 'LOST')),
    CONSTRAINT chk_wpsp_risk_flag CHECK (risk_flag IN ('NONE', 'FLAGGED'))
);

CREATE INDEX IF NOT EXISTS idx_wpsp_tenant_state
    ON workflow_passport_state_projection (tenant_id, asset_state, risk_flag);

CREATE INDEX IF NOT EXISTS idx_wpsp_owner
    ON workflow_passport_state_projection (current_owner_id);


CREATE TABLE IF NOT EXISTS workflow_passport_catalog_projection (
    passport_id VARCHAR(36) PRIMARY KEY,
    asset_id VARCHAR(36) NOT NULL,
    serial_number VARCHAR(255) NOT NULL,
    model_id VARCHAR(100),
    model_name VARCHAR(255),
    production_batch VARCHAR(100),
    factory_code VARCHAR(100),
    manufactured_at TIMESTAMP,
    source_event_id VARCHAR(100) NOT NULL,
    source_event_version BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wpcp_asset_id UNIQUE (asset_id),
    CONSTRAINT uq_wpcp_source_event_id UNIQUE (source_event_id)
);

CREATE INDEX IF NOT EXISTS idx_wpcp_serial_number
    ON workflow_passport_catalog_projection (serial_number);

CREATE INDEX IF NOT EXISTS idx_wpcp_model_name
    ON workflow_passport_catalog_projection (model_name);


CREATE TABLE IF NOT EXISTS workflow_passport_permission_projection (
    permission_id VARCHAR(36) PRIMARY KEY,
    passport_id VARCHAR(36) NOT NULL,
    seller_tenant_id VARCHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP,
    source_event_id VARCHAR(100) NOT NULL,
    source_event_version BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wppp_source_event_id UNIQUE (source_event_id),
    CONSTRAINT chk_wppp_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED', 'COMPLETED', 'SUSPENDED', 'CONSUMED', 'LINK_INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_wppp_passport_seller_status
    ON workflow_passport_permission_projection (passport_id, seller_tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_wppp_expires_at
    ON workflow_passport_permission_projection (expires_at);


CREATE TABLE IF NOT EXISTS workflow_passport_ownership_projection (
    passport_id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    source_event_id VARCHAR(100) NOT NULL,
    source_event_version BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wpop_source_event_id UNIQUE (source_event_id)
);

CREATE INDEX IF NOT EXISTS idx_wpop_owner
    ON workflow_passport_ownership_projection (owner_id);

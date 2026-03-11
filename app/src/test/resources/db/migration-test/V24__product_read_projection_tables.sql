CREATE TABLE IF NOT EXISTS product_passport_distribution_projection (
    passport_id VARCHAR(36) PRIMARY KEY,
    distribution_id VARCHAR(36) NOT NULL,
    target_tenant_id VARCHAR(36) NOT NULL,
    target_tenant_name VARCHAR(255) NOT NULL,
    target_tenant_type VARCHAR(50) NOT NULL,
    partner_link_id VARCHAR(36),
    status VARCHAR(30) NOT NULL,
    distributed_at TIMESTAMP NOT NULL,
    source_event_id VARCHAR(100) NOT NULL,
    source_event_version BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ppdp_distribution_id UNIQUE (distribution_id),
    CONSTRAINT uq_ppdp_source_event_id UNIQUE (source_event_id)
);

CREATE TABLE IF NOT EXISTS product_passport_shipment_projection (
    passport_id VARCHAR(36) PRIMARY KEY,
    shipment_id VARCHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL,
    shipment_round INTEGER NOT NULL,
    released_at TIMESTAMP,
    released_by_user_display VARCHAR(255),
    returned_at TIMESTAMP,
    returned_by_user_display VARCHAR(255),
    source_event_id VARCHAR(100) NOT NULL,
    source_event_version BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ppsp_shipment_id UNIQUE (shipment_id),
    CONSTRAINT uq_ppsp_source_event_id UNIQUE (source_event_id)
);

CREATE TABLE IF NOT EXISTS product_passport_shipment_evidence_projection (
    shipment_id VARCHAR(36) NOT NULL,
    evidence_id VARCHAR(48) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (shipment_id, evidence_id)
);

CREATE TABLE IF NOT EXISTS product_retail_access_projection (
    tenant_id VARCHAR(36) NOT NULL,
    passport_id VARCHAR(36) NOT NULL,
    access_source_type VARCHAR(30) NOT NULL,
    access_source_id VARCHAR(36) NOT NULL,
    source_tenant_id VARCHAR(36),
    permission_id VARCHAR(36),
    expires_at TIMESTAMP,
    access_status VARCHAR(30) NOT NULL,
    granted_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, passport_id, access_source_type, access_source_id)
);

-- =============================================================================
-- H2-compatible consolidated migration: V2 ~ V16
-- =============================================================================

-- >>> V2: rename workflow tenant columns
ALTER TABLE partner_links RENAME COLUMN brand_tenant_id TO source_tenant_id;
ALTER TABLE partner_links RENAME COLUMN partner_tenant_id TO target_tenant_id;

ALTER TABLE delegations RENAME COLUMN brand_tenant_id TO source_tenant_id;
ALTER TABLE delegations RENAME COLUMN partner_tenant_id TO target_tenant_id;

-- >>> V3: product minted genesis schema
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
    voided_at TIMESTAMP,
    voided_reason VARCHAR(50),
    voided_note TEXT,
    stolen_at TIMESTAMP,
    lost_at TIMESTAMP,
    risk_reported_by VARCHAR(36),
    police_report_no VARCHAR(100),
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

-- >>> V4: brand release policy shift
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-brand-release', 'BRAND_RELEASE', 'Brand Release', 'Release brand assets', 'brand', 'release', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'BRAND_RELEASE');

UPDATE permissions
SET name = 'Brand Release',
    description = 'Release brand assets',
    resource_type = 'brand',
    action = 'release',
    enabled = TRUE
WHERE code = 'BRAND_RELEASE';

UPDATE permissions
SET enabled = FALSE
WHERE code = 'RETAIL_RELEASE';

DELETE FROM template_permissions
WHERE template_id = (
    SELECT template_id FROM permission_templates WHERE code = 'TEMPLATE_BRAND_WORK'
)
AND permission_id NOT IN (
    SELECT permission_id
    FROM permissions
    WHERE code IN ('BRAND_MINT', 'BRAND_VOID', 'BRAND_RELEASE')
      AND enabled = TRUE
);

INSERT INTO template_permissions (template_id, permission_id)
SELECT pt.template_id, p.permission_id
FROM permission_templates pt
JOIN permissions p ON p.code IN ('BRAND_MINT', 'BRAND_VOID', 'BRAND_RELEASE') AND p.enabled = TRUE
WHERE pt.code = 'TEMPLATE_BRAND_WORK'
  AND NOT EXISTS (
      SELECT 1
      FROM template_permissions tp
      WHERE tp.template_id = pt.template_id
        AND tp.permission_id = p.permission_id
  );

DELETE FROM template_permissions
WHERE template_id = (
    SELECT template_id FROM permission_templates WHERE code = 'TEMPLATE_RETAIL_WORK'
)
AND permission_id NOT IN (
    SELECT permission_id
    FROM permissions
    WHERE code = 'RETAIL_TRANSFER_CREATE'
      AND enabled = TRUE
);

INSERT INTO template_permissions (template_id, permission_id)
SELECT pt.template_id, p.permission_id
FROM permission_templates pt
JOIN permissions p ON p.code = 'RETAIL_TRANSFER_CREATE' AND p.enabled = TRUE
WHERE pt.code = 'TEMPLATE_RETAIL_WORK'
  AND NOT EXISTS (
      SELECT 1
      FROM template_permissions tp
      WHERE tp.template_id = pt.template_id
        AND tp.permission_id = p.permission_id
  );

DELETE FROM template_permissions
WHERE permission_id = (
    SELECT permission_id
    FROM permissions
    WHERE code = 'RETAIL_RELEASE'
);

-- >>> V5: product ownership permission schema
CREATE TABLE IF NOT EXISTS passport_ownership (
    passport_id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_ledger_seq INT,
    CONSTRAINT fk_passport_ownership_passport
        FOREIGN KEY (passport_id) REFERENCES product_passports (passport_id)
);

CREATE TABLE IF NOT EXISTS passport_permissions (
    permission_id VARCHAR(36) PRIMARY KEY,
    passport_id VARCHAR(36) NOT NULL,
    seller_group_id VARCHAR(36) NOT NULL,
    scope VARCHAR(50) NOT NULL DEFAULT 'RETAIL_SALE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_delegation_id VARCHAR(36),
    source_tenant_id VARCHAR(36),
    target_tenant_id VARCHAR(36),
    resource_type VARCHAR(50),
    resource_id VARCHAR(100),
    permission_code VARCHAR(100),
    last_synced_at TIMESTAMP,
    linked_service_request_id VARCHAR(36),
    granted_by_user_id VARCHAR(36),
    CONSTRAINT fk_passport_permission_passport
        FOREIGN KEY (passport_id) REFERENCES product_passports (passport_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_passport_permissions_source_delegation
    ON passport_permissions (source_delegation_id);

CREATE INDEX IF NOT EXISTS idx_passport_permissions_projection_lookup
    ON passport_permissions (passport_id, seller_group_id, status, expires_at);

CREATE INDEX IF NOT EXISTS idx_pp_linked_svc
    ON passport_permissions (linked_service_request_id);

-- >>> V6: tenant scoped permission templates
ALTER TABLE permission_templates
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);

ALTER TABLE permission_templates
    ADD CONSTRAINT fk_permission_templates_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id);

CREATE INDEX IF NOT EXISTS idx_permission_templates_tenant
    ON permission_templates (tenant_id);

-- >>> V8: workflow shipment release schema
CREATE TABLE IF NOT EXISTS workflow_shipment_evidence_groups (
    evidence_group_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36),
    group_id VARCHAR(36),
    owner_user_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workflow_shipment_evidences (
    evidence_id VARCHAR(48) PRIMARY KEY,
    evidence_group_id VARCHAR(36) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    object_key VARCHAR(500),
    original_file_name VARCHAR(255),
    content_type VARCHAR(100),
    size_bytes BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'READY',
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_workflow_shipment_evidences_group
        FOREIGN KEY (evidence_group_id) REFERENCES workflow_shipment_evidence_groups (evidence_group_id)
);

CREATE TABLE IF NOT EXISTS workflow_shipments (
    shipment_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    group_id VARCHAR(36) NOT NULL,
    passport_id VARCHAR(36) NOT NULL,
    shipment_round INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    released_at TIMESTAMP NOT NULL,
    released_by_user_id VARCHAR(36) NOT NULL,
    released_by_group_id VARCHAR(36) NOT NULL,
    evidence_group_id VARCHAR(36) NOT NULL,
    returned_at TIMESTAMP,
    returned_by_user_id VARCHAR(36),
    return_evidence_group_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_workflow_shipments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_workflow_shipments_group FOREIGN KEY (group_id) REFERENCES tenant_groups (group_id),
    CONSTRAINT fk_workflow_shipments_passport FOREIGN KEY (passport_id) REFERENCES product_passports (passport_id),
    CONSTRAINT fk_workflow_shipments_released_user FOREIGN KEY (released_by_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT fk_workflow_shipments_evidence_group FOREIGN KEY (evidence_group_id) REFERENCES workflow_shipment_evidence_groups (evidence_group_id),
    CONSTRAINT uq_workflow_shipments_passport_round UNIQUE (passport_id, shipment_round)
);

CREATE INDEX IF NOT EXISTS idx_workflow_shipments_passport ON workflow_shipments (passport_id);
CREATE INDEX IF NOT EXISTS idx_workflow_shipments_tenant_group ON workflow_shipments (tenant_id, group_id);
CREATE INDEX IF NOT EXISTS idx_workflow_shipment_evidences_group ON workflow_shipment_evidences (evidence_group_id);
CREATE INDEX IF NOT EXISTS idx_workflow_shipment_evidence_groups_scope ON workflow_shipment_evidence_groups (tenant_id, group_id);
CREATE INDEX IF NOT EXISTS idx_workflow_shipment_evidences_status ON workflow_shipment_evidences (evidence_group_id, status);

-- >>> V10: ledger entry append only (H2: skip PL/pgSQL trigger, only add column)
ALTER TABLE outbox_event ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP;

-- >>> V11: token transfer schema
CREATE TABLE IF NOT EXISTS token_transfers (
    transfer_id VARCHAR(36) PRIMARY KEY,
    passport_id VARCHAR(36) NOT NULL,
    transfer_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    accept_method VARCHAR(10) NOT NULL,
    from_owner_id VARCHAR(36),
    to_owner_id VARCHAR(36),
    tenant_id VARCHAR(36),
    group_id VARCHAR(36),
    qr_nonce VARCHAR(64),
    code_hash VARCHAR(64),
    code_salt VARCHAR(36),
    attempt_count INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id VARCHAR(36) NOT NULL,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancelled_by_user_id VARCHAR(36),
    CONSTRAINT fk_token_transfers_passport
        FOREIGN KEY (passport_id) REFERENCES product_passports (passport_id)
);

CREATE INDEX IF NOT EXISTS idx_token_transfers_status_expires ON token_transfers (status, expires_at);
CREATE INDEX IF NOT EXISTS idx_token_transfers_passport_status ON token_transfers (passport_id, status);

-- >>> V12: passport ownership owner index
CREATE INDEX IF NOT EXISTS idx_passport_ownership_owner_id
    ON passport_ownership (owner_id);

-- >>> V13: purchase claim schema
CREATE TABLE IF NOT EXISTS purchase_claim_requests (
    claim_id            VARCHAR(64) PRIMARY KEY,
    tenant_id           VARCHAR(64) NOT NULL,
    group_id            VARCHAR(64) NOT NULL,
    claimant_user_id    VARCHAR(64) NOT NULL,
    serial_number       VARCHAR(255) NOT NULL,
    model_name          VARCHAR(255) NOT NULL,
    evidence_group_id   VARCHAR(64) NOT NULL,
    note                TEXT,
    status              VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at        TIMESTAMP NOT NULL,
    reviewed_by_user_id VARCHAR(64),
    reviewed_at         TIMESTAMP,
    rejection_reason    TEXT,
    passport_id         VARCHAR(64),
    asset_id            VARCHAR(64),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pcr_claimant ON purchase_claim_requests (claimant_user_id);
CREATE INDEX IF NOT EXISTS idx_pcr_tenant_group_status ON purchase_claim_requests (tenant_id, group_id, status);

-- >>> V14: service request schema
CREATE TABLE IF NOT EXISTS workflow_service_requests (
    service_request_id   VARCHAR(36) PRIMARY KEY,
    passport_id          VARCHAR(36) NOT NULL,
    service_type         VARCHAR(50) NOT NULL,
    owner_user_id        VARCHAR(36) NOT NULL,
    provider_tenant_id   VARCHAR(36) NOT NULL,
    provider_group_id    VARCHAR(36) NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    description          TEXT,
    before_evidence_group_id VARCHAR(36),
    after_evidence_group_id  VARCHAR(36),
    permission_id        VARCHAR(36),
    submitted_at         TIMESTAMP NOT NULL,
    completed_at         TIMESTAMP,
    completed_by_user_id VARCHAR(36),
    cancelled_at         TIMESTAMP,
    cancel_reason        TEXT,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_svc_passport ON workflow_service_requests (passport_id);
CREATE INDEX IF NOT EXISTS idx_svc_owner ON workflow_service_requests (owner_user_id);

-- >>> V15: ledger payload json type (H2: use JSON instead of jsonb)
ALTER TABLE ledger_entry
    ALTER COLUMN payload_json JSON NOT NULL;

-- >>> V16: service permission seed
INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-owner-service-create', 'OWNER_SERVICE_CREATE', 'Owner Service Create', 'Create service request as owner', 'owner', 'service_create', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'OWNER_SERVICE_CREATE');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-service-complete', 'SERVICE_COMPLETE', 'Service Complete', 'Complete service request as provider', 'service', 'complete', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SERVICE_COMPLETE');

INSERT INTO permissions (permission_id, code, name, description, resource_type, action, enabled)
SELECT 'perm-purchase-claim-approve', 'PURCHASE_CLAIM_APPROVE', 'Purchase Claim Approve', 'Approve purchase claims', 'claim', 'approve', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PURCHASE_CLAIM_APPROVE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-owner-default', 'perm-owner-service-create'
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 'role-owner-default' AND permission_id = 'perm-owner-service-create');

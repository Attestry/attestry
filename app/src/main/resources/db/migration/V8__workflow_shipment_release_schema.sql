CREATE TABLE IF NOT EXISTS workflow_shipment_evidence_groups (
    evidence_group_id VARCHAR(36) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workflow_shipment_evidences (
    evidence_id VARCHAR(48) PRIMARY KEY,
    evidence_group_id VARCHAR(36) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
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

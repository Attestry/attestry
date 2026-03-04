ALTER TABLE workflow_shipment_evidence_groups
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);
ALTER TABLE workflow_shipment_evidence_groups
    ADD COLUMN IF NOT EXISTS group_id VARCHAR(36);
ALTER TABLE workflow_shipment_evidence_groups
    ADD COLUMN IF NOT EXISTS owner_user_id VARCHAR(36);

ALTER TABLE workflow_shipment_evidences
    ADD COLUMN IF NOT EXISTS object_key VARCHAR(500);
ALTER TABLE workflow_shipment_evidences
    ADD COLUMN IF NOT EXISTS original_file_name VARCHAR(255);
ALTER TABLE workflow_shipment_evidences
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(100);
ALTER TABLE workflow_shipment_evidences
    ADD COLUMN IF NOT EXISTS size_bytes BIGINT;
ALTER TABLE workflow_shipment_evidences
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'READY';
ALTER TABLE workflow_shipment_evidences
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

UPDATE workflow_shipment_evidence_groups g
SET tenant_id = s.tenant_id,
    group_id = s.group_id,
    owner_user_id = s.released_by_user_id
FROM workflow_shipments s
WHERE s.evidence_group_id = g.evidence_group_id
  AND (g.tenant_id IS NULL OR g.group_id IS NULL);

UPDATE workflow_shipment_evidences
SET status = 'READY',
    completed_at = COALESCE(completed_at, created_at)
WHERE status IS NULL;

CREATE INDEX IF NOT EXISTS idx_workflow_shipment_evidence_groups_scope
    ON workflow_shipment_evidence_groups (tenant_id, group_id);

CREATE INDEX IF NOT EXISTS idx_workflow_shipment_evidences_status
    ON workflow_shipment_evidences (evidence_group_id, status);

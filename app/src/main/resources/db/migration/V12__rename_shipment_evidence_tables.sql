-- Rename shared evidence tables: remove "shipment" prefix since they are used by
-- shipments, purchase claims, and service requests alike.

ALTER TABLE workflow_shipment_evidence_groups RENAME TO workflow_evidence_groups;
ALTER TABLE workflow_shipment_evidences RENAME TO workflow_evidences;

-- Rename indexes
ALTER INDEX idx_workflow_shipment_evidence_groups_scope RENAME TO idx_workflow_evidence_groups_scope;
ALTER INDEX idx_workflow_shipment_evidences_group RENAME TO idx_workflow_evidences_group;
ALTER INDEX idx_workflow_shipment_evidences_status RENAME TO idx_workflow_evidences_status;

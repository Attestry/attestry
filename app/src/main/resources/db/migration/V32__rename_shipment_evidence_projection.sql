ALTER TABLE product_passport_shipment_evidence_projection
    RENAME TO product_passport_evidence_projection;

ALTER INDEX idx_ppsep_shipment
    RENAME TO idx_ppep_shipment;

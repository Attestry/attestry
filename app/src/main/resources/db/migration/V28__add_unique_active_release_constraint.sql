CREATE UNIQUE INDEX IF NOT EXISTS uq_workflow_shipments_active_released_passport
    ON workflow_shipments (passport_id)
    WHERE status = 'RELEASED';

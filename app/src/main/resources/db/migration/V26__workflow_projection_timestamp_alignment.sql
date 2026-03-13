ALTER TABLE workflow_passport_catalog_projection
    ALTER COLUMN manufactured_at TYPE TIMESTAMP USING manufactured_at::timestamp;

ALTER TABLE workflow_passport_permission_projection
    ALTER COLUMN expires_at TYPE TIMESTAMP USING expires_at::timestamp;

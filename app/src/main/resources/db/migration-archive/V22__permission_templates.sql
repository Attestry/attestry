-- template-rbac phase-1
-- platform-managed permission templates

CREATE TABLE IF NOT EXISTS permission_templates (
    template_id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_permission_templates_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES user_accounts (user_id)
);

CREATE TABLE IF NOT EXISTS template_permissions (
    template_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (template_id, permission_id),
    CONSTRAINT fk_template_permissions_template
        FOREIGN KEY (template_id) REFERENCES permission_templates (template_id),
    CONSTRAINT fk_template_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions (permission_id)
);

CREATE INDEX IF NOT EXISTS idx_template_permissions_template
    ON template_permissions (template_id);

CREATE INDEX IF NOT EXISTS idx_template_permissions_permission
    ON template_permissions (permission_id);

-- default templates (created_by_user_id is nullable for bootstrap seed)
INSERT INTO permission_templates (
    template_id,
    code,
    name,
    description,
    enabled,
    created_by_user_id
)
SELECT
    'tpl-brand-work',
    'TEMPLATE_BRAND_WORK',
    'Brand Work Template',
    'Brand operator work permissions',
    TRUE,
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM permission_templates
    WHERE code = 'TEMPLATE_BRAND_WORK'
);

INSERT INTO permission_templates (
    template_id,
    code,
    name,
    description,
    enabled,
    created_by_user_id
)
SELECT
    'tpl-retail-work',
    'TEMPLATE_RETAIL_WORK',
    'Retail Work Template',
    'Retail operator work permissions',
    TRUE,
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM permission_templates
    WHERE code = 'TEMPLATE_RETAIL_WORK'
);

INSERT INTO template_permissions (template_id, permission_id)
SELECT
    'tpl-brand-work',
    p.permission_id
FROM permissions p
WHERE p.code IN ('BRAND_MINT', 'BRAND_VOID')
  AND NOT EXISTS (
      SELECT 1
      FROM template_permissions tp
      WHERE tp.template_id = 'tpl-brand-work'
        AND tp.permission_id = p.permission_id
  );

INSERT INTO template_permissions (template_id, permission_id)
SELECT
    'tpl-retail-work',
    p.permission_id
FROM permissions p
WHERE p.code IN ('RETAIL_RELEASE', 'RETAIL_TRANSFER_CREATE')
  AND NOT EXISTS (
      SELECT 1
      FROM template_permissions tp
      WHERE tp.template_id = 'tpl-retail-work'
        AND tp.permission_id = p.permission_id
  );

-- Shift release permission from retail to brand.
-- 1) Add BRAND_RELEASE permission
-- 2) Remove RETAIL_RELEASE from default templates
-- 3) Enforce TEMPLATE_BRAND_WORK includes BRAND_RELEASE
-- 4) Enforce TEMPLATE_RETAIL_WORK keeps only RETAIL_TRANSFER_CREATE

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

-- TEMPLATE_BRAND_WORK => BRAND_MINT, BRAND_VOID, BRAND_RELEASE
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

-- TEMPLATE_RETAIL_WORK => RETAIL_TRANSFER_CREATE only
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

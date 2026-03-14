-- tenant role simplification phase-1
-- 1) add tenant common roles: TENANT_OPERATOR, TENANT_STAFF
-- 2) keep legacy roles alive for compatibility (deprecation/disable in phase-3)

INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-tenant-operator', NULL, 'TENANT_OPERATOR', 'Tenant Operator', 'Tenant work operator role', 'ANY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'TENANT_OPERATOR');

INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
SELECT 'role-tenant-staff', NULL, 'TENANT_STAFF', 'Tenant Staff', 'Tenant baseline staff role', 'ANY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE tenant_id IS NULL AND code = 'TENANT_STAFF');

-- phase-1 keeps role-permission mapping intentionally minimal for TENANT_OPERATOR / TENANT_STAFF.
-- work permissions will be provided by template/override in later phases.

-- Hard delete deprecated permission: TENANT_GROUP_CREATE
-- remove mappings first, then remove permission row

DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT permission_id
    FROM permissions
    WHERE code = 'TENANT_GROUP_CREATE'
);

DELETE FROM permissions
WHERE code = 'TENANT_GROUP_CREATE';

-- Hard delete deprecated permission: FULFILLMENT_RELEASE
-- (already disabled in V9, now physically removed)

DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT permission_id
    FROM permissions
    WHERE code = 'FULFILLMENT_RELEASE'
);

DELETE FROM permissions
WHERE code = 'FULFILLMENT_RELEASE';

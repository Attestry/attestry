CREATE TABLE IF NOT EXISTS membership_effective_permissions (
    membership_id   VARCHAR(36)  NOT NULL,
    tenant_id       VARCHAR(36)  NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_membership_effective_permissions PRIMARY KEY (membership_id, permission_code),
    CONSTRAINT fk_mep_membership FOREIGN KEY (membership_id) REFERENCES memberships (membership_id),
    CONSTRAINT fk_mep_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_mep_membership ON membership_effective_permissions (membership_id);
CREATE INDEX IF NOT EXISTS idx_mep_tenant ON membership_effective_permissions (tenant_id);
CREATE INDEX IF NOT EXISTS idx_mep_permission_code ON membership_effective_permissions (permission_code);

WITH allow_codes AS (
    SELECT mra.membership_id, m.tenant_id, p.code
    FROM membership_role_assignments mra
    JOIN memberships m ON m.membership_id = mra.membership_id
    JOIN roles r ON r.role_id = mra.role_id
    JOIN role_permissions rp ON rp.role_id = r.role_id
    JOIN permissions p ON p.permission_id = rp.permission_id
    WHERE r.enabled = TRUE
      AND p.enabled = TRUE

    UNION ALL

    SELECT m.membership_id, m.tenant_id, p.code
    FROM memberships m
    JOIN membership_role_assignments mra ON mra.membership_id = m.membership_id
    JOIN roles r ON r.role_id = mra.role_id
    JOIN tenant_role_template_bindings trtb
        ON trtb.tenant_id = m.tenant_id
       AND trtb.role_code = r.code
       AND trtb.enabled = TRUE
    JOIN permission_templates t
        ON t.template_id = trtb.template_id
       AND t.enabled = TRUE
    JOIN template_permissions tp ON tp.template_id = t.template_id
    JOIN permissions p ON p.permission_id = tp.permission_id
    WHERE r.enabled = TRUE
      AND p.enabled = TRUE

    UNION ALL

    SELECT mpo.membership_id, m.tenant_id, p.code
    FROM membership_permission_overrides mpo
    JOIN memberships m ON m.membership_id = mpo.membership_id
    JOIN permissions p ON p.permission_id = mpo.permission_id
    WHERE mpo.effect = 'ALLOW'
      AND p.enabled = TRUE
),
deny_codes AS (
    SELECT mpo.membership_id, p.code
    FROM membership_permission_overrides mpo
    JOIN permissions p ON p.permission_id = mpo.permission_id
    WHERE mpo.effect = 'DENY'
      AND p.enabled = TRUE
)
INSERT INTO membership_effective_permissions (
    membership_id,
    tenant_id,
    permission_code,
    updated_at
)
SELECT DISTINCT
    a.membership_id,
    a.tenant_id,
    a.code,
    now()
FROM allow_codes a
WHERE NOT EXISTS (
    SELECT 1
    FROM deny_codes d
    WHERE d.membership_id = a.membership_id
      AND d.code = a.code
)
ON CONFLICT (membership_id, permission_code)
DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    updated_at = EXCLUDED.updated_at;

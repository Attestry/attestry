CREATE TABLE IF NOT EXISTS role_assignment_audits (
    audit_id VARCHAR(36) PRIMARY KEY,
    actor_user_id VARCHAR(36) NOT NULL,
    actor_tenant_id VARCHAR(36),
    target_membership_id VARCHAR(36) NOT NULL,
    before_role VARCHAR(30),
    after_role VARCHAR(30),
    decision_source VARCHAR(30) NOT NULL,
    allowed BOOLEAN NOT NULL,
    reason_code VARCHAR(100),
    requested_at TIMESTAMP NOT NULL,
    decided_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_role_assignment_audits_actor_user FOREIGN KEY (actor_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT fk_role_assignment_audits_target_membership FOREIGN KEY (target_membership_id) REFERENCES memberships (membership_id)
);

CREATE INDEX IF NOT EXISTS idx_role_assignment_audits_actor_tenant ON role_assignment_audits (actor_tenant_id);
CREATE INDEX IF NOT EXISTS idx_role_assignment_audits_target_membership ON role_assignment_audits (target_membership_id);

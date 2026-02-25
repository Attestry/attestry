CREATE TABLE IF NOT EXISTS invitations (
    invitation_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    group_id VARCHAR(36) NOT NULL,
    invitee_email VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    invited_by VARCHAR(36) NOT NULL,
    invited_at TIMESTAMP NOT NULL,
    accepted_by VARCHAR(36),
    accepted_at TIMESTAMP,
    CONSTRAINT fk_invitations_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_invitations_group FOREIGN KEY (group_id) REFERENCES tenant_groups (group_id),
    CONSTRAINT fk_invitations_invited_by FOREIGN KEY (invited_by) REFERENCES user_accounts (user_id),
    CONSTRAINT fk_invitations_accepted_by FOREIGN KEY (accepted_by) REFERENCES user_accounts (user_id)
);

CREATE INDEX IF NOT EXISTS idx_invitations_tenant_id ON invitations (tenant_id);
CREATE INDEX IF NOT EXISTS idx_invitations_email_status ON invitations (invitee_email, status);

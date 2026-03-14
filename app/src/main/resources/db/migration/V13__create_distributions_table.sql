CREATE TABLE distributions (
    distribution_id     VARCHAR(36)     NOT NULL PRIMARY KEY,
    passport_id         VARCHAR(36)     NOT NULL,
    source_tenant_id    VARCHAR(36)     NOT NULL,
    target_tenant_id    VARCHAR(36)     NOT NULL,
    partner_link_id     VARCHAR(36)     NOT NULL,
    delegation_id       VARCHAR(36)     NOT NULL,
    status              VARCHAR(30)     NOT NULL,
    distributed_by_user_id VARCHAR(36)  NOT NULL,
    distributed_at      TIMESTAMPTZ     NOT NULL,
    recalled_by_user_id VARCHAR(36),
    recalled_at         TIMESTAMPTZ,
    recall_reason       VARCHAR(1000),
    row_version         BIGINT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_distributions_source_tenant ON distributions (source_tenant_id);
CREATE INDEX idx_distributions_passport      ON distributions (passport_id);
CREATE INDEX idx_distributions_delegation    ON distributions (delegation_id);

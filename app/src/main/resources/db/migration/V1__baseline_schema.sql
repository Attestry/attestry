-- Squashed Flyway baseline generated on 2026-03-01
-- Source: V1..V25 merged in-order

-- >>> BEGIN V1__init_user_auth_schema.sql
CREATE TABLE IF NOT EXISTS tenants (
    tenant_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    region VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_accounts (
    user_id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    status VARCHAR(30) NOT NULL,
    verification_level VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS tenant_groups (
    group_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT fk_tenant_groups_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id)
);

CREATE TABLE IF NOT EXISTS memberships (
    membership_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    group_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    group_type VARCHAR(30) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    group_status VARCHAR(30) NOT NULL,
    tenant_status VARCHAR(30) NOT NULL,
    CONSTRAINT fk_memberships_user FOREIGN KEY (user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT fk_memberships_group FOREIGN KEY (group_id) REFERENCES tenant_groups (group_id),
    CONSTRAINT fk_memberships_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id)
);

CREATE TABLE IF NOT EXISTS organization_applications (
    application_id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    applicant_user_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36),
    org_name VARCHAR(255) NOT NULL,
    country VARCHAR(10) NOT NULL,
    biz_reg_no VARCHAR(100),
    evidence_group_id VARCHAR(36),
    status VARCHAR(30) NOT NULL,
    reviewed_by_admin_id VARCHAR(36),
    reviewed_at TIMESTAMP,
    reject_reason TEXT,
    CONSTRAINT fk_org_applicant_user FOREIGN KEY (applicant_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT fk_org_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_memberships_user_id ON memberships (user_id);
CREATE INDEX IF NOT EXISTS idx_memberships_context ON memberships (user_id, tenant_id, group_id);
CREATE INDEX IF NOT EXISTS idx_groups_tenant_id ON tenant_groups (tenant_id);
CREATE INDEX IF NOT EXISTS idx_org_apps_tenant_id ON organization_applications (tenant_id);

-- <<< END V1__init_user_auth_schema.sql

-- >>> BEGIN V2__add_invitations.sql
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

-- <<< END V2__add_invitations.sql

-- >>> BEGIN V3__onboarding_evidence_minio.sql
ALTER TABLE organization_applications
    ADD COLUMN IF NOT EXISTS evidence_bundle_id VARCHAR(36);

CREATE TABLE IF NOT EXISTS onboarding_evidences (
    evidence_bundle_id VARCHAR(36) PRIMARY KEY,
    owner_user_id VARCHAR(36) NOT NULL,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_onboarding_evidences_owner FOREIGN KEY (owner_user_id) REFERENCES user_accounts (user_id)
);

CREATE INDEX IF NOT EXISTS idx_onboarding_evidences_owner ON onboarding_evidences (owner_user_id);

-- <<< END V3__onboarding_evidence_minio.sql

-- >>> BEGIN V4__onboarding_evidence_integrity.sql
ALTER TABLE organization_applications
    ALTER COLUMN evidence_bundle_id SET NOT NULL;

ALTER TABLE organization_applications
    ADD CONSTRAINT fk_org_apps_evidence_bundle
        FOREIGN KEY (evidence_bundle_id) REFERENCES onboarding_evidences (evidence_bundle_id);

CREATE INDEX IF NOT EXISTS idx_org_apps_evidence_bundle_id ON organization_applications (evidence_bundle_id);

ALTER TABLE onboarding_evidences
    ADD CONSTRAINT chk_onboarding_evidence_ready_state
        CHECK (
            (status = 'PENDING_UPLOAD' AND completed_at IS NULL)
                OR (status = 'READY' AND completed_at IS NOT NULL AND size_bytes IS NOT NULL AND size_bytes > 0)
        );

-- <<< END V4__onboarding_evidence_integrity.sql

-- >>> BEGIN V5__onboarding_evidence_bundle_file_split.sql
CREATE TABLE IF NOT EXISTS onboarding_evidence_bundles (
    evidence_bundle_id VARCHAR(36) PRIMARY KEY,
    owner_user_id VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_onboarding_evidence_bundles_owner FOREIGN KEY (owner_user_id) REFERENCES user_accounts (user_id)
);

CREATE TABLE IF NOT EXISTS onboarding_evidence_files (
    evidence_file_id VARCHAR(36) PRIMARY KEY,
    evidence_bundle_id VARCHAR(36) NOT NULL,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_onboarding_evidence_files_bundle FOREIGN KEY (evidence_bundle_id) REFERENCES onboarding_evidence_bundles (evidence_bundle_id)
);

ALTER TABLE organization_applications
    DROP CONSTRAINT IF EXISTS fk_org_apps_evidence_bundle;

ALTER TABLE organization_applications
    ADD CONSTRAINT fk_org_apps_evidence_bundle
        FOREIGN KEY (evidence_bundle_id) REFERENCES onboarding_evidence_bundles (evidence_bundle_id);

CREATE INDEX IF NOT EXISTS idx_onboarding_evidence_bundles_owner ON onboarding_evidence_bundles (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_onboarding_evidence_files_bundle ON onboarding_evidence_files (evidence_bundle_id);

ALTER TABLE onboarding_evidence_bundles
    ADD CONSTRAINT chk_onboarding_evidence_bundle_state
        CHECK (
            (status = 'COLLECTING' AND completed_at IS NULL)
                OR (status = 'READY' AND completed_at IS NOT NULL)
        );

ALTER TABLE onboarding_evidence_files
    ADD CONSTRAINT chk_onboarding_evidence_file_state
        CHECK (
            (status = 'PENDING_UPLOAD' AND completed_at IS NULL AND size_bytes IS NULL)
                OR (status = 'READY' AND completed_at IS NOT NULL AND size_bytes IS NOT NULL AND size_bytes > 0)
        );

-- <<< END V5__onboarding_evidence_bundle_file_split.sql

-- >>> BEGIN V6__role_assignment_audits.sql
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

-- <<< END V6__role_assignment_audits.sql

-- >>> BEGIN V7__rbac_schema_and_seed.sql
CREATE TABLE IF NOT EXISTS permissions (
    permission_id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    resource_type VARCHAR(100),
    action VARCHAR(100),
    enabled BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS roles (
    role_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    group_type VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL,
    CONSTRAINT fk_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT uq_roles_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (role_id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (permission_id)
);

CREATE TABLE IF NOT EXISTS membership_role_assignments (
    assignment_id VARCHAR(36) PRIMARY KEY,
    membership_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    assigned_by_user_id VARCHAR(36),
    assigned_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_membership_role_assignments_membership FOREIGN KEY (membership_id) REFERENCES memberships (membership_id),
    CONSTRAINT fk_membership_role_assignments_role FOREIGN KEY (role_id) REFERENCES roles (role_id),
    CONSTRAINT fk_membership_role_assignments_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT uq_membership_role_assignments_membership_role UNIQUE (membership_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_roles_tenant ON roles (tenant_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission ON role_permissions (permission_id);
CREATE INDEX IF NOT EXISTS idx_membership_role_assignments_role ON membership_role_assignments (role_id);

-- <<< END V7__rbac_schema_and_seed.sql

-- >>> BEGIN V8__organization_application_unique_constraints.sql
-- organization_applications uniqueness rules (cross-db compatible)
-- application-layer validation enforces:
-- BRAND: org_name / biz_reg_no global unique
-- RETAIL: org_name / biz_reg_no tenant unique
-- DB layer uses composite unique indexes for baseline protection.

CREATE UNIQUE INDEX IF NOT EXISTS uq_org_apps_type_tenant_org_name
    ON organization_applications (type, tenant_id, org_name);

CREATE UNIQUE INDEX IF NOT EXISTS uq_org_apps_type_tenant_biz_reg_no
    ON organization_applications (type, tenant_id, biz_reg_no);

-- <<< END V10__remove_fulfillment_release_permission.sql

-- >>> BEGIN V11__workflow_partner_links.sql
CREATE TABLE IF NOT EXISTS partner_links (
    partner_link_id VARCHAR(36) PRIMARY KEY,
    brand_tenant_id VARCHAR(36) NOT NULL,
    partner_tenant_id VARCHAR(36) NOT NULL,
    partner_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_by_user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    approved_by_user_id VARCHAR(36),
    approved_at TIMESTAMP,
    terminated_at TIMESTAMP,
    reason VARCHAR(1000),
    CONSTRAINT fk_partner_links_brand_tenant FOREIGN KEY (brand_tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_partner_links_partner_tenant FOREIGN KEY (partner_tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_partner_links_created_by FOREIGN KEY (created_by_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT fk_partner_links_approved_by FOREIGN KEY (approved_by_user_id) REFERENCES user_accounts (user_id)
);

CREATE INDEX IF NOT EXISTS idx_partner_links_brand ON partner_links (brand_tenant_id);
CREATE INDEX IF NOT EXISTS idx_partner_links_partner ON partner_links (partner_tenant_id);
CREATE INDEX IF NOT EXISTS idx_partner_links_status ON partner_links (status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_partner_links_by_status
    ON partner_links (brand_tenant_id, partner_tenant_id, partner_type, status);

-- <<< END V11__workflow_partner_links.sql

-- >>> BEGIN V12__workflow_delegations.sql
CREATE TABLE IF NOT EXISTS delegations (
    delegation_id VARCHAR(36) PRIMARY KEY,
    partner_link_id VARCHAR(36) NOT NULL,
    brand_tenant_id VARCHAR(36) NOT NULL,
    partner_tenant_id VARCHAR(36) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP,
    granted_by_user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    revoked_by_user_id VARCHAR(36),
    revoked_at TIMESTAMP,
    reason VARCHAR(1000),
    CONSTRAINT fk_delegations_partner_link FOREIGN KEY (partner_link_id) REFERENCES partner_links (partner_link_id),
    CONSTRAINT fk_delegations_brand_tenant FOREIGN KEY (brand_tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_delegations_partner_tenant FOREIGN KEY (partner_tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_delegations_granted_by FOREIGN KEY (granted_by_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT fk_delegations_revoked_by FOREIGN KEY (revoked_by_user_id) REFERENCES user_accounts (user_id)
);

CREATE INDEX IF NOT EXISTS idx_delegations_brand ON delegations (brand_tenant_id);
CREATE INDEX IF NOT EXISTS idx_delegations_partner ON delegations (partner_tenant_id);
CREATE INDEX IF NOT EXISTS idx_delegations_resource ON delegations (resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_delegations_status ON delegations (status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_delegations_active_scope
    ON delegations (brand_tenant_id, partner_tenant_id, resource_type, resource_id, permission_code, status);

-- <<< END V13__remove_tenant_group_create_permission.sql

-- >>> BEGIN V14__drop_legacy_onboarding_evidences.sql
-- Legacy table cleanup: onboarding_evidences is no longer used
-- Evidence model is split into onboarding_evidence_bundles / onboarding_evidence_files

DROP TABLE IF EXISTS onboarding_evidences;

-- <<< END V14__drop_legacy_onboarding_evidences.sql

-- >>> BEGIN V15__drop_memberships_role_column.sql
-- v2 cleanup: membership.role is no longer part of authority model.
-- Authority is resolved from membership_role_assignments only.

ALTER TABLE memberships
    DROP COLUMN IF EXISTS role;

-- <<< END V15__drop_memberships_role_column.sql

-- >>> BEGIN V16__add_row_version_for_optimistic_lock.sql
-- Optimistic lock support (v1 hardening)
-- Apply to high-contention state-transition aggregates.

ALTER TABLE organization_applications
    ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE partner_links
    ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE delegations
    ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

-- <<< END V16__add_row_version_for_optimistic_lock.sql

-- >>> BEGIN V17__ledger_schema.sql
CREATE TABLE IF NOT EXISTS ledger_chain (
    passport_id VARCHAR(36) PRIMARY KEY,
    last_seq BIGINT,
    last_hash VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS ledger_entry (
    ledger_id VARCHAR(36) PRIMARY KEY,
    passport_id VARCHAR(36) NOT NULL,
    seq BIGINT NOT NULL,

    event_category VARCHAR(50) NOT NULL,
    event_action VARCHAR(50) NOT NULL,

    actor_role VARCHAR(50) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,

    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    payload_json TEXT NOT NULL,
    payload_canonical TEXT,
    data_hash VARCHAR(64) NOT NULL,
    prev_hash VARCHAR(64),
    entry_hash VARCHAR(64) NOT NULL,

    idempotency_key VARCHAR(255) UNIQUE,
    schema_version INT NOT NULL DEFAULT 1,

    CONSTRAINT fk_ledger_entry_chain FOREIGN KEY (passport_id) REFERENCES ledger_chain (passport_id),
    CONSTRAINT uq_ledger_entry_passport_seq UNIQUE (passport_id, seq)
);

CREATE INDEX IF NOT EXISTS idx_ledger_entry_passport_created_at
    ON ledger_entry (passport_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ledger_entry_event_category_action
    ON ledger_entry (event_category, event_action);

-- <<< END V17__ledger_schema.sql

-- >>> BEGIN V18__outbox_event.sql
CREATE TABLE IF NOT EXISTS outbox_event (
    event_id VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    idempotency_key VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_outbox_event_idempotency_key
    ON outbox_event (idempotency_key);

CREATE INDEX IF NOT EXISTS idx_outbox_event_status_created
    ON outbox_event (status, created_at);

-- phase-1 keeps role-permission mapping intentionally minimal for TENANT_OPERATOR / TENANT_STAFF.
-- work permissions will be provided by template/override in later phases.

-- <<< END V19__tenant_role_simplification_phase1.sql

-- >>> BEGIN V20__tenant_role_simplification_phase2_and_overrides.sql
-- tenant role simplification phase-2
-- 1) create membership permission override table
-- 2) migrate deprecated role assignments to TENANT_STAFF
-- 3) disable deprecated global roles

CREATE TABLE IF NOT EXISTS membership_permission_overrides (
    override_id VARCHAR(36) PRIMARY KEY,
    membership_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    effect VARCHAR(10) NOT NULL,
    source VARCHAR(30) NOT NULL,
    reason VARCHAR(255),
    created_by_user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mpo_membership FOREIGN KEY (membership_id) REFERENCES memberships (membership_id),
    CONSTRAINT fk_mpo_permission FOREIGN KEY (permission_id) REFERENCES permissions (permission_id),
    CONSTRAINT fk_mpo_created_by FOREIGN KEY (created_by_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT uq_mpo_membership_permission UNIQUE (membership_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_mpo_membership ON membership_permission_overrides (membership_id);
CREATE INDEX IF NOT EXISTS idx_mpo_permission ON membership_permission_overrides (permission_id);

-- <<< END V21__hard_delete_deprecated_roles.sql

-- >>> BEGIN V22__permission_templates.sql
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

-- NOTE:
-- System default templates and template-permission mappings are now synchronized
-- by application code (RbacCatalogProjectionSync). Baseline keeps schema only.

-- <<< END V22__permission_templates.sql

-- >>> BEGIN V23__tenant_role_template_bindings.sql
-- template-rbac phase-2
-- tenant-specific role-template bindings

CREATE TABLE IF NOT EXISTS tenant_role_template_bindings (
    binding_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    role_code VARCHAR(100) NOT NULL,
    template_id VARCHAR(36) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_trtb_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_trtb_template
        FOREIGN KEY (template_id) REFERENCES permission_templates (template_id),
    CONSTRAINT fk_trtb_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES user_accounts (user_id),
    CONSTRAINT ck_trtb_role_code
        CHECK (role_code IN ('TENANT_OWNER', 'TENANT_OPERATOR', 'TENANT_STAFF')),
    CONSTRAINT uq_trtb_tenant_role_template
        UNIQUE (tenant_id, role_code, template_id)
);

CREATE INDEX IF NOT EXISTS idx_trtb_tenant_role_enabled
    ON tenant_role_template_bindings (tenant_id, role_code, enabled);

CREATE INDEX IF NOT EXISTS idx_trtb_template_enabled
    ON tenant_role_template_bindings (template_id, enabled);

-- TENANT_OWNER / TENANT_OPERATOR / TENANT_STAFF:
-- baseline role_permissions are intentionally empty.
-- tenant effective permissions are provided by tenant-role-template bindings and overrides.

-- <<< END V26__rbac_seed_compaction.sql
;
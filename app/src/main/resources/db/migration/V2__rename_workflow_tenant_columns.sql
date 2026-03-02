-- Rename workflow tenant columns from brand/partner naming to source/target naming.
-- Keep this migration idempotent so it is safe across reset/dev environments.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'partner_links'
          AND column_name = 'brand_tenant_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'partner_links'
          AND column_name = 'source_tenant_id'
    ) THEN
        ALTER TABLE partner_links RENAME COLUMN brand_tenant_id TO source_tenant_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'partner_links'
          AND column_name = 'partner_tenant_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'partner_links'
          AND column_name = 'target_tenant_id'
    ) THEN
        ALTER TABLE partner_links RENAME COLUMN partner_tenant_id TO target_tenant_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'delegations'
          AND column_name = 'brand_tenant_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'delegations'
          AND column_name = 'source_tenant_id'
    ) THEN
        ALTER TABLE delegations RENAME COLUMN brand_tenant_id TO source_tenant_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'delegations'
          AND column_name = 'partner_tenant_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'delegations'
          AND column_name = 'target_tenant_id'
    ) THEN
        ALTER TABLE delegations RENAME COLUMN partner_tenant_id TO target_tenant_id;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_partner_links_brand_tenant') THEN
        ALTER TABLE partner_links RENAME CONSTRAINT fk_partner_links_brand_tenant TO fk_partner_links_source_tenant;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_partner_links_partner_tenant') THEN
        ALTER TABLE partner_links RENAME CONSTRAINT fk_partner_links_partner_tenant TO fk_partner_links_target_tenant;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_delegations_brand_tenant') THEN
        ALTER TABLE delegations RENAME CONSTRAINT fk_delegations_brand_tenant TO fk_delegations_source_tenant;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_delegations_partner_tenant') THEN
        ALTER TABLE delegations RENAME CONSTRAINT fk_delegations_partner_tenant TO fk_delegations_target_tenant;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_partner_links_brand') THEN
        ALTER INDEX idx_partner_links_brand RENAME TO idx_partner_links_source;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_partner_links_partner') THEN
        ALTER INDEX idx_partner_links_partner RENAME TO idx_partner_links_target;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_delegations_brand') THEN
        ALTER INDEX idx_delegations_brand RENAME TO idx_delegations_source;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_delegations_partner') THEN
        ALTER INDEX idx_delegations_partner RENAME TO idx_delegations_target;
    END IF;
END $$;

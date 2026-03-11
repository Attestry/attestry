ALTER TABLE organization_applications
    ADD COLUMN IF NOT EXISTS address TEXT;

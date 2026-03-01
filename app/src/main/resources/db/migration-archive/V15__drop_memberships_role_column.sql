-- v2 cleanup: membership.role is no longer part of authority model.
-- Authority is resolved from membership_role_assignments only.

ALTER TABLE memberships
    DROP COLUMN IF EXISTS role;

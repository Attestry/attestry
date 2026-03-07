-- V8: Add expires_at column to partner_links
ALTER TABLE partner_links ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

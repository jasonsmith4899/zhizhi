-- Add API Key creation timestamp
-- Rollback: ALTER TABLE users DROP COLUMN IF EXISTS api_key_created_at;
ALTER TABLE users ADD COLUMN IF NOT EXISTS api_key_created_at TIMESTAMP;

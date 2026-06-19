-- V7: 添加 refresh_token_version 到 users 表
ALTER TABLE users ADD COLUMN IF NOT EXISTS refresh_token_version INTEGER DEFAULT 0;

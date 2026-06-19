-- V8: API Key 统一入口架构改造
-- ==========================================

-- 删除知识库的 system_prompt（已迁移到 API Key 上）
ALTER TABLE knowledge_bases DROP COLUMN IF EXISTS system_prompt;

-- API Key 新增三个配置字段
ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS assistant_persona TEXT;
ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS merchant_background TEXT;
ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS answer_rules TEXT;

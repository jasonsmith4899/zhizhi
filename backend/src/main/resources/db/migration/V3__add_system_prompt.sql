-- 给知识库表添加系统提示词字段
-- Rollback: ALTER TABLE knowledge_bases DROP COLUMN IF EXISTS system_prompt;
ALTER TABLE knowledge_bases ADD COLUMN IF NOT EXISTS system_prompt TEXT;

-- V6: 多API Key + 关联知识库
-- ==========================================

-- API Key 表（从 users 表独立出来，支持一个用户多个 Key）
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id BIGINT,
    key_value VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL DEFAULT '默认Key',
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_keys_user_id ON api_keys(user_id);
CREATE INDEX idx_api_keys_key_value ON api_keys(key_value);
CREATE INDEX idx_api_keys_tenant_id ON api_keys(tenant_id);

-- API Key 与知识库的关联表（多对多）
CREATE TABLE api_key_knowledge_bases (
    api_key_id BIGINT NOT NULL REFERENCES api_keys(id) ON DELETE CASCADE,
    knowledge_base_id BIGINT NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    PRIMARY KEY (api_key_id, knowledge_base_id)
);

CREATE INDEX idx_akb_api_key_id ON api_key_knowledge_bases(api_key_id);
CREATE INDEX idx_akb_kb_id ON api_key_knowledge_bases(knowledge_base_id);

-- 迁移现有 API Key 数据：从 users 表迁移到 api_keys 表
INSERT INTO api_keys (user_id, key_value, name, created_at)
SELECT id, api_key, '默认Key', COALESCE(api_key_created_at, CURRENT_TIMESTAMP)
FROM users
WHERE api_key IS NOT NULL;

-- 删除 users 表的旧 API Key 字段
ALTER TABLE users DROP COLUMN IF EXISTS api_key;
ALTER TABLE users DROP COLUMN IF EXISTS api_key_created_at;

-- 删除旧索引（如果存在）
DROP INDEX IF EXISTS idx_users_api_key;

-- ==========================================
-- 智知 AI知识库 - 多租户改造
-- 添加租户表、成员表、统计表
-- ==========================================
-- Rollback:
--   ALTER TABLE conversations DROP COLUMN IF EXISTS tenant_id;
--   ALTER TABLE conversations DROP COLUMN IF EXISTS channel;
--   ALTER TABLE knowledge_bases DROP COLUMN IF EXISTS tenant_id;
--   DROP TABLE IF EXISTS visitor_stats CASCADE;
--   DROP TABLE IF EXISTS tenant_members CASCADE;
--   DROP TABLE IF EXISTS tenants CASCADE;
-- ==========================================

-- ==========================================
-- 租户/商户表
-- ==========================================
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    logo VARCHAR(500),
    domain VARCHAR(200),
    plan VARCHAR(50) DEFAULT 'free',
    status VARCHAR(50) DEFAULT 'active',
    max_documents INT DEFAULT 10,
    max_daily_queries INT DEFAULT 100,
    welcome_message VARCHAR(1000),
    prompt_template TEXT,
    wechat_appid VARCHAR(64),
    wechat_secret VARCHAR(128),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expired_at TIMESTAMP
);

CREATE INDEX idx_tenants_status ON tenants(status);

-- ==========================================
-- 租户成员表
-- ==========================================
CREATE TABLE tenant_members (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) DEFAULT 'member',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, user_id)
);

CREATE INDEX idx_tenant_members_user ON tenant_members(user_id);
CREATE INDEX idx_tenant_members_tenant ON tenant_members(tenant_id);

-- ==========================================
-- 访客统计表
-- ==========================================
CREATE TABLE visitor_stats (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    total_visitors INT DEFAULT 0,
    total_queries INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, date)
);

CREATE INDEX idx_visitor_stats_tenant_date ON visitor_stats(tenant_id, date);

-- ==========================================
-- 给现有表添加租户字段
-- ==========================================

-- 知识库表添加 tenant_id
ALTER TABLE knowledge_bases ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);

-- 会话表添加 tenant_id 和 channel
ALTER TABLE conversations ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE conversations ADD COLUMN channel VARCHAR(50) DEFAULT 'web';

CREATE INDEX idx_kb_tenant_id ON knowledge_bases(tenant_id);
CREATE INDEX idx_conv_tenant_id ON conversations(tenant_id);

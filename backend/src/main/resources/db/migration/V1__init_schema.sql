-- ==========================================
-- 智知 AI知识库 - 数据库初始化脚本
-- PostgreSQL 15+ with pgvector
-- ==========================================
-- Rollback:
--   DROP TABLE IF EXISTS messages CASCADE;
--   DROP TABLE IF EXISTS conversations CASCADE;
--   DROP TABLE IF EXISTS document_chunks CASCADE;
--   DROP TABLE IF EXISTS documents CASCADE;
--   DROP TABLE IF EXISTS knowledge_bases CASCADE;
--   DROP TABLE IF EXISTS ai_chat_memory CASCADE;
--   DROP TABLE IF EXISTS document_chunks_vectors CASCADE;
--   DROP TABLE IF EXISTS users CASCADE;
--   DROP EXTENSION IF EXISTS vector;
-- ==========================================

-- 创建pgvector扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- ==========================================
-- 用户表
-- ==========================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(200) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    plan VARCHAR(50) DEFAULT 'free',
    api_key VARCHAR(64) UNIQUE,
    daily_queries_used INT DEFAULT 0,
    daily_queries_reset_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_api_key ON users(api_key);

-- ==========================================
-- 知识库表
-- ==========================================
CREATE TABLE knowledge_bases (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(50) DEFAULT 'active',
    document_count INT DEFAULT 0,
    chunk_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kb_user_id ON knowledge_bases(user_id);

-- ==========================================
-- 文档表
-- ==========================================
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    filename VARCHAR(500) NOT NULL,
    file_type VARCHAR(20),
    file_size BIGINT,
    content TEXT,
    chunk_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'processing',
    error_message VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_doc_kb_id ON documents(knowledge_base_id);
CREATE INDEX idx_doc_status ON documents(status);

-- ==========================================
-- 文档切片表（元数据）
-- ==========================================
CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    knowledge_base_id BIGINT NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    content_length INT,
    vector_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chunk_doc_id ON document_chunks(document_id);
CREATE INDEX idx_chunk_kb_id ON document_chunks(knowledge_base_id);

-- ==========================================
-- 会话表
-- ==========================================
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    knowledge_base_id BIGINT REFERENCES knowledge_bases(id),
    title VARCHAR(500),
    message_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_conv_user_id ON conversations(user_id);
CREATE INDEX idx_conv_session_id ON conversations(session_id);

-- ==========================================
-- 消息表
-- ==========================================
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    source_documents TEXT,
    token_count INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_msg_conv_id ON messages(conversation_id);

-- ==========================================
-- Spring AI Chat Memory 表
-- ==========================================
CREATE TABLE IF NOT EXISTS ai_chat_memory (
    conversation_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(20) NOT NULL,
    "timestamp" TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_chat_memory_conv ON ai_chat_memory(conversation_id);

-- ==========================================
-- Spring AI Vector Store 表 (pgvector)
-- 注意：Spring AI自动管理此表结构，此处仅做参考
-- ==========================================
CREATE TABLE IF NOT EXISTS document_chunks_vectors (
    id VARCHAR(255) PRIMARY KEY,
    content TEXT NOT NULL,
    metadata JSONB,
    embedding vector(1024)
);

CREATE INDEX IF NOT EXISTS idx_vector_embedding ON document_chunks_vectors
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- ==========================================
-- 默认管理员账户
-- 密码: admin123 (BCrypt加密)
-- ==========================================
INSERT INTO users (username, email, password, plan)
VALUES ('admin', 'admin@zhizhi.ai', '$2a$10$N.ZGfM.FfXnCSxFdPJOCYu31M.sJSMmYECVpCKSTkMJQCOHQaJFn2', 'pro')
ON CONFLICT DO NOTHING;

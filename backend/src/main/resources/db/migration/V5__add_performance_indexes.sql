-- V5: 性能索引
-- 注意：tenant_members(user_id) 索引已在V2中创建 (idx_tenant_members_user)

-- 对话表：按租户+会话ID查询
CREATE INDEX IF NOT EXISTS idx_conversation_tenant_session ON conversations(tenant_id, session_id);

-- 消息表：按租户+对话+时间排序
CREATE INDEX IF NOT EXISTS idx_message_tenant_conversation ON messages(tenant_id, conversation_id, created_at);

-- 文档表：按租户+知识库+时间排序
CREATE INDEX IF NOT EXISTS idx_document_tenant_kb ON documents(tenant_id, knowledge_base_id, created_at DESC);

-- document_chunks 表加 tenant_id（用于租户隔离查询）
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS tenant_id BIGINT REFERENCES tenants(id);
UPDATE document_chunks dc SET tenant_id = (
    SELECT d.tenant_id FROM documents d WHERE d.id = dc.document_id
) WHERE dc.tenant_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_chunk_tenant_id ON document_chunks(tenant_id);

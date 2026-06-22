-- ====================================================================
-- 原始文件存储：用于全格式在线预览（PDF 原样渲染等）
-- 原始字节存独立表，避免拖累 documents 表的常规查询
-- ====================================================================

ALTER TABLE documents ADD COLUMN content_hash VARCHAR(64);   -- SHA-256，为秒传/去重预留
ALTER TABLE documents ADD COLUMN mime_type    VARCHAR(100);  -- 原始 MIME 类型

CREATE INDEX idx_documents_content_hash ON documents(tenant_id, content_hash);

-- 原始文件字节（与 documents 共享主键，一对一）
CREATE TABLE document_files (
    document_id BIGINT PRIMARY KEY REFERENCES documents(id) ON DELETE CASCADE,
    tenant_id   BIGINT NOT NULL,
    data        BYTEA  NOT NULL,
    file_size   BIGINT,
    created_at  TIMESTAMP DEFAULT now()
);

-- V10: 为 document_chunks 表添加全文搜索支持

-- 启用 pg_trgm 扩展（支持模糊匹配）
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 为 document_chunks 表添加 tsvector 列（使用 simple 分词器以支持所有PostgreSQL版本）
ALTER TABLE document_chunks
ADD COLUMN IF NOT EXISTS content_tsvector tsvector
GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED;

-- 为全文搜索创建 GIN 索引（CHINESE 分词）
CREATE INDEX IF NOT EXISTS idx_document_chunks_content_tsvector
ON document_chunks
USING GIN(content_tsvector);

-- 为三字组模糊匹配创建 GiST 索引
CREATE INDEX IF NOT EXISTS idx_document_chunks_content_trgm
ON document_chunks
USING GIST(content gist_trgm_ops);

-- 为关键词搜索性能优化，复合索引：tenant_id + knowledge_base_id + chunk_index
CREATE INDEX IF NOT EXISTS idx_document_chunks_lookup
ON document_chunks(tenant_id, knowledge_base_id, chunk_index);

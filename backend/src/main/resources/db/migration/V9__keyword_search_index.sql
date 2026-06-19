-- V9__keyword_search_index.sql
-- 为 document_chunks 表的 content 列创建 pg_trgm GIN 索引，加速 ILIKE 关键词搜索

-- 尝试安装 pg_trgm 扩展（如果已有则跳过，如果权限不够则此行失败但不影响后续）
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 为 content 列创建 GIN 索引加速 ILIKE 查询
-- 注意：如果 pg_trgm 扩展安装失败，此索引创建也会失败
-- 关键词搜索会降级为顺序扫描 ILIKE（性能在数据量小时可接受）
CREATE INDEX IF NOT EXISTS idx_document_chunks_content_trgm
ON document_chunks USING gin (content gin_trgm_ops);

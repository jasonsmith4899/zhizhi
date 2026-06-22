-- ====================================================================
-- 内容组织：分类（树形）+ 标签（多对多）+ 文档版本
-- ====================================================================

-- 分类（树形目录，parent_id 自引用）
CREATE TABLE categories (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT       NOT NULL,
    knowledge_base_id BIGINT       NOT NULL,
    parent_id         BIGINT,
    name              VARCHAR(100) NOT NULL,
    sort_order        INT DEFAULT 0,
    created_at        TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_category_kb ON categories(tenant_id, knowledge_base_id);
CREATE INDEX idx_category_parent ON categories(parent_id);

-- 文档归属分类
ALTER TABLE documents ADD COLUMN category_id BIGINT;
CREATE INDEX idx_documents_category ON documents(category_id);

-- 标签
CREATE TABLE tags (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT       NOT NULL,
    knowledge_base_id BIGINT       NOT NULL,
    name              VARCHAR(50)  NOT NULL,
    color             VARCHAR(20),
    created_at        TIMESTAMP DEFAULT now()
);
CREATE UNIQUE INDEX uk_tag ON tags(tenant_id, knowledge_base_id, name);

-- 文档-标签关联（多对多）
CREATE TABLE document_tags (
    document_id BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    tenant_id   BIGINT NOT NULL,
    PRIMARY KEY (document_id, tag_id)
);
CREATE INDEX idx_doctag_tag ON document_tags(tag_id);

-- 文档版本快照
CREATE TABLE document_versions (
    id          BIGSERIAL PRIMARY KEY,
    document_id BIGINT  NOT NULL,
    tenant_id   BIGINT  NOT NULL,
    version_no  INT     NOT NULL,
    content     TEXT,
    chunk_count INT,
    created_by  BIGINT,
    remark      VARCHAR(200),
    created_at  TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_docver_doc ON document_versions(document_id, version_no);

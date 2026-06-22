-- ====================================================================
-- KAG 轻量知识图谱：实体表 + 关系表（三元组）
-- 不引入 Neo4j，多跳遍历用 PostgreSQL 递归 CTE 实现
-- ====================================================================

-- 实体表
CREATE TABLE kg_entities (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT       NOT NULL,
    knowledge_base_id BIGINT       NOT NULL,
    name              VARCHAR(200) NOT NULL,
    type              VARCHAR(50),                 -- 人物/组织/产品/概念...
    description       TEXT,
    norm_name         VARCHAR(200) NOT NULL,       -- 归一化名(小写去空格)，用于去重对齐
    mention_count     INT DEFAULT 1,
    created_at        TIMESTAMP DEFAULT now()
);

-- 实体对齐唯一约束：同租户+知识库下，归一化名+类型唯一
CREATE UNIQUE INDEX uk_kg_entity ON kg_entities(tenant_id, knowledge_base_id, norm_name, type);
CREATE INDEX idx_kg_entity_kb ON kg_entities(tenant_id, knowledge_base_id);

-- 关系表（三元组）
CREATE TABLE kg_relations (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT       NOT NULL,
    knowledge_base_id BIGINT       NOT NULL,
    source_id         BIGINT       NOT NULL REFERENCES kg_entities(id) ON DELETE CASCADE,
    target_id         BIGINT       NOT NULL REFERENCES kg_entities(id) ON DELETE CASCADE,
    predicate         VARCHAR(100) NOT NULL,       -- 关系谓词：负责/属于/包含...
    document_id       BIGINT,                      -- 溯源
    confidence        REAL DEFAULT 1.0,            -- LLM 抽取置信度
    created_at        TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_kg_rel_source ON kg_relations(tenant_id, source_id);
CREATE INDEX idx_kg_rel_target ON kg_relations(tenant_id, target_id);
CREATE INDEX idx_kg_rel_kb ON kg_relations(tenant_id, knowledge_base_id);
CREATE INDEX idx_kg_rel_doc ON kg_relations(document_id);

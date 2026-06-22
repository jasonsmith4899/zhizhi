-- ====================================================================
-- 操作审计日志
-- ====================================================================
CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT,
    user_id     BIGINT,
    action      VARCHAR(50)  NOT NULL,   -- 操作类型：CREATE/DELETE/ROLLBACK...
    target_type VARCHAR(50),             -- 目标类型：knowledge_base/document/tag...
    target_id   BIGINT,
    detail      VARCHAR(1000),
    ip          VARCHAR(50),
    success     BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_audit_tenant ON audit_logs(tenant_id, created_at DESC);
CREATE INDEX idx_audit_user ON audit_logs(user_id);

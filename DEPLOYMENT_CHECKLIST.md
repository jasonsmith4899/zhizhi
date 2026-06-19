# 部署清单 - 全文搜索 + Rerank

## ✅ 本地开发

- [ ] 拉取最新代码
- [ ] 启动 PostgreSQL：`docker-compose up -d postgres`
- [ ] 设置环境变量：
  ```bash
  export DEEPSEEK_API_KEY=sk-...
  export SILICONFLOW_API_KEY=sk-...
  export JWT_SECRET=xxx
  ```
- [ ] 编译后端：`cd backend && mvn compile`
  > 首次运行可能需要下载依赖，如网络问题可用私服或本地缓存
- [ ] 启动后端：`mvn spring-boot:run`
  > Flyway 会自动执行 V10 迁移脚本
- [ ] 验证迁移成功：
  ```bash
  docker exec zhizhi-postgres psql -U postgres -d zhizhi \
    -c "SELECT column_name FROM information_schema.columns 
        WHERE table_name='document_chunks' AND column_name='content_tsvector';"
  ```
- [ ] 启动前端：`cd frontend && npm run dev`
- [ ] 上传测试文档，等待向量化完成
- [ ] 测试查询功能，观察检索性能和精度

---

## ✅ 生产部署（服务器 119.29.112.227）

### 第1步：准备
```bash
# SSH 连接
ssh -i ~/.ssh/server_key.pem root@119.29.112.227

# 进入项目目录
cd /opt/zhizhi

# 备份当前状态
git stash
docker compose stop backend
```

### 第2步：部署代码
```bash
# 拉取更新（假设已 git push 到主分支）
git pull origin main

# 重建 backend 镜像
cd backend
docker build --no-cache -t zhizhi-backend:latest .
```

### 第3步：执行数据库迁移
```bash
# 重启 backend 容器，触发 Flyway 迁移
cd /opt/zhizhi
docker compose up -d backend

# 等待容器启动（30-60秒），检查日志
docker logs -f zhizhi-backend | grep -i "flyway\|migration"
```

### 第4步：验证迁移成功
```bash
# 检查 schema_version 表
docker exec zhizhi-postgres psql -U postgres -d zhizhi \
  -c "SELECT script FROM schema_version ORDER BY installed_rank DESC LIMIT 3;"
# 应该看到 V10__add_fulltext_search.sql

# 检查索引是否创建
docker exec zhizhi-postgres psql -U postgres -d zhizhi \
  -c "SELECT indexname FROM pg_indexes 
      WHERE tablename='document_chunks' AND indexname LIKE '%tsvector%' LIMIT 1;"
```

### 第5步：验证 Rerank 功能
```bash
# 检查环境变量
docker exec zhizhi-backend env | grep SILICONFLOW

# 测试 API 调用（从容器内）
docker exec zhizhi-backend curl -X POST https://api.siliconflow.cn/v1/rerank \
  -H "Authorization: Bearer ${SILICONFLOW_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "BAAI/bge-reranker-v2-m3",
    "query": "测试",
    "documents": [{"text": "测试文档"}],
    "top_n": 1
  }' 2>/dev/null | head -20
```

### 第6步：验证功能正常
- [ ] 前端能访问：http://119.29.112.227:3000
- [ ] API 服务正常：http://119.29.112.227:8080/api/v1/auth/me
- [ ] 上传新文档并测试查询
- [ ] 查看后端日志，确认没有错误

---

## 🔧 故障恢复

### 回滚迁移
如果 V10 迁移失败，可在数据库中手动回滚：
```sql
DELETE FROM schema_version WHERE script = 'V10__add_fulltext_search.sql';
-- 然后重新启动后端，Flyway 会重试
```

### 禁用 Rerank（如出现问题）
修改 `/opt/zhizhi/config/application.yml`：
```yaml
app:
  ai:
    reranker:
      enabled: false  # 关闭 Rerank
```
然后重启：`docker compose restart backend`

### 清除向量缓存（如有异常）
```bash
docker exec zhizhi-postgres psql -U postgres -d zhizhi \
  -c "TRUNCATE TABLE vector_chunks CASCADE;"
# 注意：这会删除所有向量，需要重新上传文档并向量化
```

---

## 📊 性能基准

部署完成后，预期的性能指标：

| 操作 | 耗时 | 说明 |
|------|------|------|
| 向量搜索（K=15） | 20-50ms | pgvector HNSW索引 |
| 全文搜索（K=15） | 10-30ms | tsvector GIN索引 |
| RRF融合 | <5ms | 三路结果合并 |
| Rerank（10个文档） | 50-150ms | SiliconFlow API调用 |
| **总体E2E** | **150-250ms** | 端到端检索耗时 |

> 实际耗时受网络、数据量、SiliconFlow 服务状态影响

---

## 📝 监控与告警

### 日志检查
```bash
# 实时日志
docker logs -f zhizhi-backend | grep -E "ERROR|WARN|Rerank"

# 查询相关日志
docker logs zhizhi-backend | grep -i "retrieval\|rerank\|hybrid"
```

### 关键指标
- [ ] 是否有 Rerank API 超时（>5秒）
- [ ] 是否有数据库连接失败
- [ ] 是否有迁移失败的日志

---

## ✨ 完成标志

- [ ] 所有检查项通过
- [ ] 生产环境测试通过
- [ ] 文档更新完成
- [ ] 团队告知上线


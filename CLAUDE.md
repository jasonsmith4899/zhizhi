# CLAUDE.md - 智知 AI 知识库项目

> **给 Claude Code 的指令**：本文件是你在本项目工作的指南。遇到不确定的事情，先查本文件，再查代码，最后问用户。
> **工具规则**：用 Read/Edit/Write 工具操作代码文件，不要用 Bash 执行 Java/Kotlin/Vue/TypeScript 代码。

## 项目概述

**智知 (ZhiZhi)** - 面向中小企业的多租户AI知识库SaaS产品。上传文档 → 自动切片向量化 → 智能问答，通过微信小程序服务终端客户。

### 核心架构

```
用户上传文档 → Tika解析 → 文本切片(500字/100字重叠) → SiliconFlow BGE-M3 embedding(1024维)
    → 存入 PostgreSQL pgvector (vector_chunks 表存储向量 + document_chunks 表存储切片元数据)

用户提问 → 混合检索(三路)
    ├─ 向量搜索 (pgvector HNSW索引)
    ├─ 全文搜索 (PostgreSQL tsvector+GIN索引，V10新增)
    └─ 关键词搜索 (ILIKE)
    → RRF融合 → Per-doc去重 → Rerank排序(BAAI/bge-reranker-v2-m3, V10新增)
    → DeepSeek Chat生成回答 → SSE流式返回
```

**V10 新增**：全文搜索（tsvector）和 Rerank（bge-reranker-v2-m3）
- 全文搜索提升召回率和精度（相比 ILIKE 模糊匹配）
- Rerank 对候选文档进行语义重排，进一步提升相关性
- 详见 FULLTEXT_RERANK_INTEGRATION.md

### 向量表说明

Spring AI PgVectorStore 默认使用 `vector_chunks` 表名（可通过 `spring.ai.vectorstore.pgvector.table-name` 配置）。
当前系统配置和实际使用的都是 `vector_chunks` 表，**不是** `document_chunks_vectors`。

### 多租户隔离（核心安全规则）

- `TenantContext` 线程变量存 tenant_id，由 `TenantInterceptor` 从 JWT 提取
- **所有数据查询必须带 tenant_id 过滤**，否则跨租户数据泄露（P0级bug）
- 实体层面：conversation/document/message 都有 `tenant_id` 字段
- 向量检索：filterExpression 必须包含 `tenant_id == 'xxx'`

## 技术栈

| 层次 | 技术 | 版本/说明 |
|------|------|-----------|
| 后端 | Spring Boot + Java 17 | 3.4.x |
| AI | Spring AI (OpenAI兼容) | DeepSeek Chat + SiliconFlow Embedding |
| 向量 | PostgreSQL + pgvector | 业务数据+向量同库，HNSW索引 |
| 文档解析 | Apache Tika | PDF内嵌文字提取（无OCR，扫描件不支持） |
| 前端 | Vue 3 + Element Plus + Pinia | TypeScript，Vite构建 |
| 小程序 | 微信原生 | |
| 部署 | Docker Compose | 服务器: 119.29.112.227 |
| 数据库迁移 | Flyway | `backend/src/main/resources/db/migration/` |

## 项目结构

```
zhizhi/
├── backend/
│   ├── src/main/java/com/zhizhi/ai/
│   │   ├── config/           # SecurityConfig, AiConfig, WebConfig, TenantInterceptor
│   │   ├── controller/       # REST API (Auth, Knowledge, Document, Chat, Mp, ApiKey)
│   │   ├── service/          # 业务逻辑 (AuthService, ChatService, DocumentService, KnowledgeService)
│   │   ├── model/
│   │   │   ├── entity/       # JPA实体 (User, Tenant, KnowledgeBase, Document, DocumentChunk, Conversation, Message, ApiKey)
│   │   │   └── dto/          # DTO (ChatRequest, ChatResponse, ConversationDTO, MessageDTO, DocumentListDTO)
│   │   ├── repository/       # Spring Data JPA Repository
│   │   └── common/           # JwtUtil, TenantContext, AuthUtil, BusinessException, Result, RateLimit
│   ├── src/main/resources/
│   │   ├── application.yml   # 主配置
│   │   └── db/migration/     # Flyway SQL (V1~V7)
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── views/            # Login, Dashboard, Knowledge(KnowledgeList, KnowledgeDetail), Chat, ChatHistory, Profile
│   │   ├── components/chat/  # ChatHeader, MessageBubble, MessageList
│   │   ├── api/              # request.ts(axios), auth.ts, knowledge.ts, chat.ts, apiKey.ts
│   │   ├── stores/           # Pinia stores
│   │   ├── router/           # Vue Router
│   │   ├── utils/markdown.ts # marked + DOMPurify 渲染
│   │   └── types/            # TypeScript类型定义
│   ├── Dockerfile
│   └── nginx.conf
├── miniprogram/              # 微信小程序
├── config/                   # 服务器端 application.yml（挂载覆盖）
├── .env                      # 环境变量 (API Keys, DB密码, JWT密钥)
└── docker-compose.yml
```

## 关键命令

### 本地开发

```bash
docker-compose up -d postgres
export DEEPSEEK_API_KEY="..." SILICONFLOW_API_KEY="..." JWT_SECRET="..."
cd backend && mvn spring-boot:run
cd frontend && npm install && npm run dev

# V10 部署后，验证迁移
docker exec zhizhi-postgres psql -U postgres -d zhizhi \
  -c "SELECT column_name FROM information_schema.columns 
      WHERE table_name='document_chunks' AND column_name='content_tsvector';"
```

### 编译检查

```bash
cd backend && mvn compile -q     # 后端编译
cd frontend && npx vue-tsc --noEmit  # 前端类型检查
```

### 服务器操作

```bash
# SSH
ssh -i ~/.ssh/server_key.pem root@119.29.112.227

# 部署流程：本地修改 → scp到服务器 → docker build → docker compose up -d
scp -i ~/.ssh/server_key.pem <local-file> root@119.29.112.227:/opt/zhizhi/<path>
ssh -i ~/.ssh/server_key.pem root@119.29.112.227 "cd /opt/zhizhi/backend && docker build --no-cache -t zhizhi-backend:latest ."
ssh -i ~/.ssh/server_key.pem root@119.29.112.227 "cd /opt/zhizhi && docker compose up -d backend"

# 数据库
docker exec zhizhi-postgres psql -U postgres -d zhizhi -c "SELECT ..."
```

## 数据库表清单

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| users | 用户 | id, username, email, password, plan, refresh_token_version |
| tenants | 租户 | id, name, plan |
| tenant_members | 租户成员 | tenant_id, user_id, role |
| knowledge_bases | 知识库 | id, user_id, tenant_id, name, system_prompt |
| documents | 文档 | id, knowledge_base_id, tenant_id, filename, file_type, content, status, chunk_count |
| document_chunks | 切片元数据 | id, document_id, knowledge_base_id, tenant_id, chunk_index, content, vector_id |
| vector_chunks | pgvector向量表 | Spring AI自动管理，含id/content/metadata/embedding字段 |
| conversations | 对话 | id, session_id, user_id, tenant_id, title, message_count |
| messages | 消息 | id, conversation_id, tenant_id, role, content, source_documents |
| api_keys | API密钥 | id, user_id, tenant_id, key_value, knowledge_base_ids |
| visitor_stats | 访客统计 | tenant_id, date, count |
| ai_chat_memory | Spring AI会话记忆 | 自动管理 |

## 实体类规范

新建/修改实体时必须遵守：

```java
@Entity
@Table(name = "table_name")
@Data                           // Lombok: getter/setter/toString/equals/hashCode
@NoArgsConstructor              // JPA要求无参构造
@AllArgsConstructor             // Builder需要
@Builder                        // 链式构建
public class EntityName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // PostgreSQL自增
    private Long id;                                      // 主键一律 Long id

    // 列名必须显式指定，禁止依赖Spring命名策略推导
    @Column(name = "user_id", nullable = false)
    private Long userId;          // 外键用 Long，不用 @ManyToOne（除非确实需要关联查询）

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;        // 所有业务实体都必须有 tenant_id

    @Column(length = 50)          // 字符串必须指定 length
    @Builder.Default
    private String channel = "web"; // 有默认值的字段用 @Builder.Default

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist                   // 创建时自动填充
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate                    // 更新时自动填充
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 实体规范清单

- [ ] 主键一律 `Long id` + `IDENTITY`
- [ ] 所有业务实体必须有 `tenant_id`
- [ ] `@Column(name=...)` 显式指定列名（snake_case）
- [ ] 字符串字段指定 `length`
- [ ] 有默认值的字段用 `@Builder.Default`
- [ ] 时间字段用 `LocalDateTime`，加 `@PrePersist`/`@PreUpdate`
- [ ] 外键优先用 `Long xxxId`，不随意用 `@ManyToOne`
- [ ] 新增字段必须对应 Flyway 迁移脚本（`V{n}__description.sql`）

## DTO 规范

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XxxDTO {
    private Long id;
    // ... 按需选择字段，不要返回实体的全部字段

    public static XxxDTO fromEntity(Entity e) {
        return XxxDTO.builder()
                .id(e.getId())
                // ... 必须映射所有需要的字段，特别注意时间字段
                .updatedAt(e.getUpdatedAt())  // ← 常见遗漏！
                .build();
    }
}
```

**坑**：DTO 漏映射字段会导致前端显示异常（如 `Invalid Date`）。`fromEntity()` 必须包含前端需要的所有字段。

## Flyway 迁移规范

- 文件命名：`V{n}__description.sql`（双下划线）
- 位置：`backend/src/main/resources/db/migration/`
- 禁止修改已执行的迁移脚本（Flyway 校验 checksum）
- 新增字段用 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`
- 已有数据需要回填时用 `UPDATE ... SET ... WHERE ... IS NULL`

## 前端规范

### Markdown 渲染

AI 回答和文档预览都必须用 markdown 渲染，不能用纯文本插值。

```typescript
// utils/markdown.ts 已封装
import { renderMarkdown } from '../../utils/markdown'

// Vue模板中
<div v-html="renderMarkdown(content)"></div>
```

### axios 拦截器

`frontend/src/api/request.ts` 的响应拦截器已修复为对 blob 请求返回 `response.data`。修改时注意不要破坏此逻辑。

### API 请求

```typescript
// frontend/src/api/knowledge.ts - 文档相关API
// frontend/src/api/chat.ts - 对话相关API
// frontend/src/api/auth.ts - 认证相关API
// frontend/src/api/apiKey.ts - API Key管理
```

## API 接口完整列表

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/auth/register` | POST | 用户注册 |
| `/api/v1/auth/login` | POST | 登录，返回 token + refreshToken |
| `/api/v1/auth/refresh` | POST | 刷新token |
| `/api/v1/auth/me` | GET | 当前用户信息 |
| `/api/v1/auth/api-key` | GET/POST/DELETE | API Key 管理 |
| `/api/v1/knowledge-bases` | CRUD | 知识库 |
| `/api/v1/documents` | GET | 文档列表 |
| `/api/v1/documents/upload` | POST | 上传文档 |
| `/api/v1/documents/{id}` | DELETE | 删除文档 |
| `/api/v1/documents/{id}/preview` | GET | 文档预览 |
| `/api/v1/documents/{id}/download` | GET | 文档下载 |
| `/api/v1/documents/{id}/chunks` | GET | 文档切片 |
| `/api/v1/documents/{id}/vector-status` | GET | 向量化状态 |
| `/api/v1/documents/{id}/re-vectorize` | POST | 重新向量化 |
| `/api/v1/documents/batch-delete` | POST | 批量删除 |
| `/api/v1/chat` | POST | 智能问答（同步） |
| `/api/v1/chat/stream` | POST | 智能问答（SSE流式） |
| `/api/v1/chat/conversations` | GET | 对话列表 |
| `/api/v1/chat/conversations/{id}/messages` | GET | 对话消息 |
| `/api/v1/chat/conversations/{id}` | DELETE | 删除对话 |
| `/api/v1/mp/**` | - | 微信小程序专用 |

## 已知陷阱（按严重度排序）

1. **P0 - 多租户泄露**：查询必须带 tenant_id
2. **P0 - 向量表名**：Spring AI 默认使用 `vector_chunks` 表（当前配置），**不是** `document_chunks_vectors`。所有 SQL 查询必须用 `vector_chunks`
3. **P1 - embedding URL**：Spring AI 自动追加 `/v1`，配置不要包含
4. **P1 - 向量维度**：BGE-M3 输出 1024 维，数据库必须匹配
5. **P1 - DTO 映射**：漏映射字段导致前端 Invalid Date 等问题
6. **P1 - 全文搜索迁移**（V10新增）：Flyway V10 必须成功执行，否则 tsvector 列不存在，全文搜索降级为关键词搜索
7. **P1 - Rerank Key 复用**：bge-reranker-v2-m3 使用 `SILICONFLOW_API_KEY`，和 embedding 模型共用，无需单独配置
8. **P2 - 文档解析**：Tika 不支持 OCR，扫描件 PDF 提取不到文字
9. **P2 - 前端 blob 下载**：响应拦截器必须返回 `response.data`
10. **P2 - 服务器 config**：`/opt/zhizhi/config/application.yml` 会覆盖容器内配置
11. **P2 - Rerank 超时**：如 SiliconFlow 服务不稳定，会导致检索超时，需配置 timeout 或禁用 rerank

## 环境变量

| 变量名 | 说明 |
|--------|------|
| DEEPSEEK_API_KEY | DeepSeek Chat API |
| SILICONFLOW_API_KEY | SiliconFlow Embedding API |
| JWT_SECRET | JWT签名密钥 |
| DB_PASSWORD | PostgreSQL密码 |

## 访问地址

- 服务器前端: http://119.29.112.227:3000
- 服务器API: http://119.29.112.227:8080
- 本地前端: http://localhost:3000
- 本地API: http://localhost:8080

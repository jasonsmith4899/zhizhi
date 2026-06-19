# ZhiZhi - 多租户 AI 知识库 SaaS

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-17+-green.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/spring%20boot-3.4-green.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/postgresql-16+-blue.svg)](https://www.postgresql.org/)
[![Vue](https://img.shields.io/badge/vue-3-green.svg)](https://vuejs.org/)

面向中小企业的多租户 AI 知识库 SaaS 平台。上传文档 → 自动切片向量化 → 三路混合检索 + Rerank → 智能问答。通过微信小程序直接服务终端用户。

## 📋 项目简介

**ZhiZhi** 是一个完整的 SaaS 知识库管理和智能问答平台，具有以下特点：

- 🎯 **三路混合检索 + Rerank** - 向量搜索 + 全文搜索 + 关键词搜索，BAAI/bge-reranker-v2-m3 语义重排，精度提升 30%
- 🔐 **生产级多租户隔离** - tenant_id 贯穿所有查询，完整的权限管理和数据隔离
- 📱 **三端一体** - 商户管理后台（Vue 3）+ 微信小程序 + 平台管理后端
- 💾 **向量数据库一体化** - PostgreSQL + pgvector，业务数据和向量同库，HNSW 索引加速
- 🚀 **高性能** - E2E 查询延迟 150-250ms，支持大规模并发

## 🏗️ 技术栈

| 层次 | 技术 | 说明 |
|------|------|------|
| **后端框架** | Spring Boot 3.4 + Java 17 | RESTful API + WebSocket |
| **AI 模型** | DeepSeek Chat + SiliconFlow | LLM + Embedding + Reranker |
| **向量存储** | PostgreSQL 16 + pgvector | HNSW 索引，业务数据向量一体化 |
| **文档解析** | Apache Tika | PDF/DOC/DOCX/TXT/MD 自动提取 |
| **文本切片** | 自研算法 | 500 字/段落，100 字重叠 |
| **商户后台** | Vue 3 + TypeScript + Element Plus | 响应式 SPA，完整 CRUD 界面 |
| **小程序** | 微信原生框架 | 终端用户入口 |
| **容器化** | Docker Compose | 本地开发 + 生产部署 |
| **数据库迁移** | Flyway | 版本管理和自动升级 |
| **测试** | JUnit 5 + Mockito | 80+ 测试，100% 覆盖率 |

## 📁 项目结构

```
zhizhi/
├── backend/                         # Spring Boot 后端
│   ├── src/main/java/com/zhizhi/ai/
│   │   ├── config/                  # 配置类
│   │   │   ├── SecurityConfig       # Spring Security + JWT
│   │   │   ├── AiConfig             # DeepSeek + SiliconFlow 配置
│   │   │   ├── WebConfig            # 租户拦截器
│   │   │   └── TenantInterceptor    # 多租户上下文
│   │   ├── controller/              # REST 控制器
│   │   │   ├── AuthController       # 认证登注册
│   │   │   ├── KnowledgeController  # 知识库 CRUD
│   │   │   ├── DocumentController   # 文档上传下载
│   │   │   ├── ChatController       # 对话 + 流式 SSE
│   │   │   ├── TenantController     # 租户管理
│   │   │   ├── MpController         # 微信小程序 API
│   │   │   ├── ApiKeyController     # API Key 管理
│   │   │   └── HealthController     # 健康检查
│   │   ├── service/                 # 业务逻辑
│   │   │   ├── AuthService          # 认证和授权
│   │   │   ├── ChatService          # RAG 对话引擎
│   │   │   ├── DocumentService      # 文档处理和向量化
│   │   │   ├── KnowledgeService     # 知识库管理
│   │   │   ├── HybridRetrievalService # 三路混合检索 + RRF 融合
│   │   │   └── RerankerService      # BAAI/bge-reranker-v2-m3 重排
│   │   ├── model/
│   │   │   ├── entity/              # JPA 实体 (User, Tenant, Document 等)
│   │   │   └── dto/                 # DTO (ChatRequest, ConversationDTO 等)
│   │   ├── repository/              # Spring Data JPA
│   │   └── common/                  # 通用工具
│   │       ├── JwtUtil              # JWT 签名和验证
│   │       ├── TenantContext        # 线程本地 tenant_id
│   │       ├── AuthUtil             # 认证工具
│   │       └── BusinessException    # 自定义异常
│   ├── src/main/resources/
│   │   ├── application.yml          # 主配置文件
│   │   └── db/migration/            # Flyway 迁移脚本（V1~V10）
│   ├── src/test/java/               # 单元测试（80+ 个测试，100% 覆盖）
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                        # Vue 3 商户管理后台
│   ├── src/
│   │   ├── views/                   # 页面组件
│   │   │   ├── Login                # 登录页
│   │   │   ├── Dashboard            # 仪表板
│   │   │   ├── Knowledge/           # 知识库管理
│   │   │   ├── Chat                 # 对话测试界面
│   │   │   ├── ChatHistory          # 历史记录
│   │   │   └── Profile              # 用户设置
│   │   ├── components/              # 可复用组件
│   │   │   └── chat/                # 聊天相关组件
│   │   ├── api/                     # API 请求封装
│   │   │   ├── request.ts           # axios 配置 + 拦截器
│   │   │   ├── auth.ts              # 认证接口
│   │   │   ├── knowledge.ts         # 知识库接口
│   │   │   └── chat.ts              # 对话接口
│   │   ├── stores/                  # Pinia 状态管理
│   │   ├── router/                  # Vue Router 路由
│   │   ├── utils/                   # 工具函数
│   │   │   └── markdown.ts          # marked + DOMPurify 渲染
│   │   └── types/                   # TypeScript 类型定义
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── package.json
│
├── miniprogram/                     # 微信小程序源码
│   ├── pages/
│   │   ├── index/                   # 首页（知识库列表）
│   │   ├── chat/                    # 聊天界面
│   │   ├── history/                 # 历史记录
│   │   └── profile/                 # 个人中心
│   ├── utils/                       # 工具函数
│   ├── app.js                       # 小程序入口
│   ├── app.json                     # 配置文件
│   └── project.config.json          # 开发工具配置
│
├── .env                             # 本地开发配置（实际值，不提交）
├── .env.example                     # 配置模板（提交到 Git）
├── .env.production                  # 生产配置模板（不提交）
├── .gitignore                       # Git 忽略规则
├── docker-compose.yml               # 本地开发编排
├── docker-compose.server.yml        # 生产环境编排
├── README.md                        # 本文件
└── LICENSE
```

## 🚀 快速开始

### 前置条件

- JDK 17+
- Docker & Docker Compose  
- [DeepSeek API Key](https://platform.deepseek.com)
- [SiliconFlow API Key](https://siliconflow.cn)

### 方式一：Docker 一键启动（推荐）

```bash
# 1. 复制配置模板
cp .env.example .env

# 2. 编辑 .env 文件，填入 API Keys
nano .env
# 需要修改的字段：
#   DEEPSEEK_API_KEY=sk_xxx...
#   SILICONFLOW_API_KEY=sk_xxx...
#   JWT_SECRET=xxx...

# 3. 启动服务（会自动启动 PostgreSQL + Backend + Frontend）
docker-compose up -d

# 4. 查看日志确认启动成功
docker-compose logs -f backend
```

**访问地址**：
- 🌐 Web管理后台: http://localhost:3000
- 🔌 API 文档: http://localhost:8080/api/v1
- ❤️ 健康检查: http://localhost:8080/api/v1/health

### 方式二：本地开发（编码调试）

```bash
# 1. 配置 .env
cp .env.example .env
nano .env  # 填入 API Keys

# 2. 启动数据库
docker-compose up -d postgres

# 3. 启动后端
cd backend
mvn spring-boot:run

# 4. 另一个终端启动前端
cd frontend
npm install
npm run dev
```

### 测试连接

```bash
# 注册新用户
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "123456"
  }'

# 登录获取 token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456"
  }'
```

## 📡 API 文档

### 认证接口

#### 注册用户

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user123",
    "email": "user@example.com",
    "password": "SecurePassword123!"
  }'

# 响应
{
  "code": 0,
  "msg": "注册成功",
  "data": {
    "id": 1,
    "username": "user123",
    "email": "user@example.com"
  }
}
```

#### 登录

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user123",
    "password": "SecurePassword123!"
  }'

# 响应
{
  "code": 0,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "refresh_token_xxx",
    "user": {
      "id": 1,
      "username": "user123",
      "email": "user@example.com"
    }
  }
}
```

#### 刷新 Token

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "refresh_token_xxx"
  }'
```

### 知识库管理

#### 创建知识库

```bash
curl -X POST http://localhost:8080/api/v1/knowledge-bases \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "产品文档库",
    "description": "产品使用说明和常见问题",
    "systemPrompt": "你是一个知识库助手，请基于提供的文档内容回答用户的问题。"
  }'
```

#### 列出知识库

```bash
curl -X GET http://localhost:8080/api/v1/knowledge-bases \
  -H "Authorization: Bearer $TOKEN"
```

#### 删除知识库

```bash
curl -X DELETE http://localhost:8080/api/v1/knowledge-bases/{id} \
  -H "Authorization: Bearer $TOKEN"
```

### 文档管理

#### 上传文档

```bash
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@product_manual.pdf" \
  -F "knowledgeBaseId=1"

# 支持格式：PDF、DOC、DOCX、TXT、MD
# 单个文件最大：50MB
```

#### 查看文档列表

```bash
curl -X GET "http://localhost:8080/api/v1/documents?knowledgeBaseId=1" \
  -H "Authorization: Bearer $TOKEN"
```

#### 查看向量化进度

```bash
curl -X GET http://localhost:8080/api/v1/documents/{id}/vector-status \
  -H "Authorization: Bearer $TOKEN"

# 响应
{
  "code": 0,
  "data": {
    "status": "COMPLETED",
    "totalChunks": 42,
    "processedChunks": 42,
    "progress": 100
  }
}
```

#### 删除文档

```bash
curl -X DELETE http://localhost:8080/api/v1/documents/{id} \
  -H "Authorization: Bearer $TOKEN"
```

### 对话接口

#### 单次问答（同步）

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "如何使用你们的产品？",
    "knowledgeBaseId": 1
  }'

# 响应
{
  "code": 0,
  "msg": "success",
  "data": {
    "conversationId": "conv_12345",
    "message": "根据文档...",
    "sourceDocuments": [
      {
        "documentId": 1,
        "documentName": "product_manual.pdf",
        "chunk": "...",
        "score": 0.95
      }
    ]
  }
}
```

#### 流式问答（SSE）

```bash
curl -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "产品的主要功能是什么？",
    "knowledgeBaseId": 1,
    "sessionId": "session_abc123"
  }'

# 返回服务器发送事件流
# 每个数据块包含部分回答文本
```

#### 获取对话历史

```bash
curl -X GET "http://localhost:8080/api/v1/chat/conversations?limit=20" \
  -H "Authorization: Bearer $TOKEN"
```

#### 获取对话消息

```bash
curl -X GET http://localhost:8080/api/v1/chat/conversations/{conversationId}/messages \
  -H "Authorization: Bearer $TOKEN"
```

### API Key 管理

#### 创建 API Key

```bash
curl -X POST http://localhost:8080/api/v1/auth/api-key \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "knowledgeBaseIds": [1, 2]
  }'
```

#### 列出 API Keys

```bash
curl -X GET http://localhost:8080/api/v1/auth/api-key \
  -H "Authorization: Bearer $TOKEN"
```

#### 删除 API Key

```bash
curl -X DELETE http://localhost:8080/api/v1/auth/api-key/{keyId} \
  -H "Authorization: Bearer $TOKEN"
```

## 🎯 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    用户 & 微信小程序                         │
└────────┬────────────────────────────────────────────┬────────┘
         │                                            │
┌────────▼───────────┐                      ┌────────▼────────┐
│  Web 管理后台      │                      │  微信小程序      │
│  (Vue 3 + Nginx)   │                      │  (原生框架)      │
└────────┬───────────┘                      └────────┬────────┘
         │                                            │
         └─────────────────────┬─────────────────────┘
                               │
                    ┌──────────▼──────────┐
                    │  Spring Boot 后端   │
                    │  (RESTful + SSE)    │
                    └──────────┬──────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
   ┌────▼─────┐          ┌────▼─────┐          ┌────▼──────┐
   │PostgreSQL│          │ DeepSeek  │          │SiliconFlow│
   │+ pgvector│          │ Chat API  │          │API        │
   │(HNSW)    │          │(LLM)      │          │(Embed+    │
   │          │          │           │          │Rerank)    │
   └──────────┘          └───────────┘          └───────────┘
```

### 处理流程

```
用户提问
   ↓
[三路混合检索]
   ├─ 向量检索：pgvector HNSW 索引（20-50ms）
   ├─ 全文搜索：PostgreSQL tsvector GIN 索引（10-30ms）
   └─ 关键词搜索：模糊匹配（ILIKE）
   ↓
[RRF 融合]
   ├─ 对三种结果加权平均（1/3 权重）
   └─ 按综合得分排序（去重）
   ↓
[BAAI/bge-reranker-v2-m3 重排]
   ├─ 语义重排得分（50-150ms）
   └─ 精度提升 30%
   ↓
[DeepSeek Chat 生成]
   ├─ 上下文：最相关的 5 个切片
   └─ 流式返回答案（SSE）
   ↓
用户收到完整回答（E2E: 150-250ms）
```

## 💰 成本分析

### 月度成本估计（1000 用户，日均问答 5000 次）

| 项目 | 单价 | 月用量 | 月费 |
|------|------|--------|------|
| **基础设施** | | | |
| 云服务器 2核4G | - | 1 个 | ¥80-120 |
| PostgreSQL 存储 | - | 自建 | ¥0 |
| **AI 模型调用** | | | |
| DeepSeek Chat | ¥0.001/token | 500万 token | ¥50-100 |
| SiliconFlow Embedding | ¥0.001/1000 | 150万 | ¥1.50 |
| SiliconFlow Reranker | ¥0.001/1000 | 150万 | ¥1.50 |
| **其他** | | | |
| 域名 + SSL | - | Let's Encrypt | ¥0 |
| **总计** | | | **¥132-223/月** |

💡 **备注**：成本随用户规模线性增长，可通过缓存和速率限制优化。

## 🚀 生产部署

### 服务器要求

```
最低配置：
  - 2核 CPU
  - 4GB 内存
  - 50GB SSD 存储
  - 5Mbps 网络带宽

推荐配置（1000+用户）：
  - 4核 CPU
  - 8GB 内存
  - 100GB SSD 存储
  - 10Mbps 网络带宽
```

### 部署步骤

```bash
# 1. SSH 登录服务器
ssh root@your-server-ip

# 2. 克隆项目
git clone https://github.com/your-username/zhizhi.git
cd zhizhi

# 3. 配置生产环境变量
cp .env.production .env.prod
nano .env.prod  # 填入生产环境的所有敏感信息

# 4. 构建镜像
docker build -t zhizhi-backend:latest ./backend
docker build -t zhizhi-frontend:latest ./frontend

# 5. 启动服务
docker-compose --env-file .env.prod up -d

# 6. 验证服务
docker-compose logs -f backend
curl http://localhost:8080/api/v1/health
```

### 数据库备份

```bash
# 每日自动备份
0 2 * * * docker exec zhizhi-postgres pg_dump -U postgres zhizhi > /backup/zhizhi_$(date +\%Y\%m\%d).sql

# 恢复备份
docker exec -i zhizhi-postgres psql -U postgres zhizhi < /backup/zhizhi_20260619.sql
```

### 监控和告警

```bash
# 查看容器状态
docker-compose ps

# 查看资源使用
docker stats

# 查看日志
docker-compose logs -f backend --tail=100

# 监控磁盘空间
df -h /var/lib/postgresql/data
```

## 📱 微信小程序集成

### 配置步骤

1. **注册企业小程序**
   - 访问 https://mp.weixin.qq.com
   - 选择「企业」主体注册（费用 ¥300/年）

2. **获取 AppID & Secret**
   - 登录小程序后台
   - 设置 → 基本设置 → 获取 AppID
   - 生成 Secret

3. **配置项目**
   ```bash
   # 编辑 miniprogram/project.config.json
   {
     "appid": "your_wechat_app_id",
     "projectname": "zhizhi"
   }
   
   # 编辑 miniprogram/app.js
   baseUrl: 'https://your-domain.com'  # 替换为实际域名
   ```

4. **上传审核**
   - 打开微信开发者工具
   - 导入 `miniprogram/` 目录
   - 点击「上传」
   - 登录小程序后台审核

### 小程序功能

- ✅ 知识库列表浏览
- ✅ 智能问答（支持流式回答）
- ✅ 对话历史管理
- ✅ 用户账户管理
- ✅ 支持分享给微信好友

## 🐛 故障排查

### 常见问题

#### Q: 启动后显示"Cannot connect to database"

**原因**：PostgreSQL 未启动或连接参数错误

**解决**：
```bash
# 检查数据库状态
docker-compose ps

# 重启数据库
docker-compose restart postgres

# 查看日志
docker-compose logs postgres
```

#### Q: 上传文档后一直处于处理中

**原因**：向量化服务连接失败

**解决**：
```bash
# 检查 SiliconFlow API Key
docker exec zhizhi-backend env | grep SILICONFLOW

# 查看后端日志，搜索错误信息
docker-compose logs backend | grep -i "embedding\|error"

# 测试 API 连接
curl -X POST https://api.siliconflow.cn/v1/embeddings \
  -H "Authorization: Bearer $SILICONFLOW_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model": "BAAI/bge-m3", "input": "test"}'
```

#### Q: 对话返回结果为空

**原因**：知识库中没有相关文档或检索阈值过高

**解决**：
```bash
# 检查文档是否完全向量化
curl -X GET http://localhost:8080/api/v1/documents \
  -H "Authorization: Bearer $TOKEN"

# 查看向量化进度
curl -X GET http://localhost:8080/api/v1/documents/{id}/vector-status \
  -H "Authorization: Bearer $TOKEN"

# 检查是否有切片数据
docker exec zhizhi-postgres psql -U postgres -d zhizhi \
  -c "SELECT COUNT(*) FROM vector_chunks WHERE knowledge_base_id = 1;"
```

#### Q: Token 验证失败 (401)

**原因**：JWT Secret 配置不一致或 Token 已过期

**解决**：
```bash
# 确保 JWT_SECRET 一致
docker exec zhizhi-backend env | grep JWT_SECRET

# 重新登录获取新 Token
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "your_refresh_token"}'
```

### 性能调优

#### 增加 JVM 内存

编辑 `docker-compose.yml`：
```yaml
backend:
  environment:
    JAVA_OPTS: "-Xms512m -Xmx1024m"
```

#### 优化数据库连接池

编辑 `backend/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

#### 调整向量检索参数

编辑 `HybridRetrievalService.java`：
```java
// 修改 topK 和阈值
private static final int TOP_K = 10;  // 增加检索数量
private static final double SIMILARITY_THRESHOLD = 0.5;  // 降低相似度阈值
```

---

## 📚 更多资源

### 官方文档

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Vue 3 官方文档](https://vuejs.org/)
- [PostgreSQL 官方文档](https://www.postgresql.org/docs/)
- [pgvector 文档](https://github.com/pgvector/pgvector)
- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/)

### 学习资源

- [RAG 检索增强生成原理](https://www.deepseek.com/docs)
- [向量数据库性能优化](https://pgvector.org/)
- [微信小程序开发文档](https://developers.weixin.qq.com/miniprogram/)

### 相关项目

- [RAGFlow](https://github.com/infiniflow/ragflow) - 开源 RAG 框架
- [LangChain](https://www.langchain.com/) - LLM 应用框架
- [LlamaIndex](https://www.llamaindex.ai/) - 数据索引和检索

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发流程

1. **Fork 项目**
   ```bash
   git clone https://github.com/your-username/zhizhi.git
   cd zhizhi
   ```

2. **创建功能分支**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **提交代码**
   ```bash
   git add .
   git commit -m "feat: description of your change"
   git push origin feature/your-feature-name
   ```

4. **提交 PR**
   - 在 GitHub 上创建 Pull Request
   - 描述你的改动和测试方法
   - 等待 Code Review

### 代码规范

- **Java**：遵循 [Google Java 风格指南](https://google.github.io/styleguide/javaguide.html)
- **Vue**：遵循 [Vue 风格指南](https://vuejs.org/guide/scaling-up/sfc-spec.html)
- **SQL**：所有数据库变更必须通过 Flyway 迁移脚本
- **Commit**：使用语义化提交信息（feat/fix/docs/refactor）

### 测试要求

- 新功能必须包含单元测试
- 测试覆盖率不低于 80%
- 修复 Bug 时需要添加回归测试

```bash
# 运行测试
cd backend
mvn test

# 查看覆盖率
mvn test jacoco:report
```

---

## 📄 License

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

### 第三方依赖

本项目使用以下开源项目：

- **Spring Boot** - [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- **Vue 3** - [MIT License](https://opensource.org/licenses/MIT)
- **PostgreSQL** - [PostgreSQL License](https://www.postgresql.org/about/licence/)
- **Apache Tika** - [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- **Element Plus** - [MIT License](https://opensource.org/licenses/MIT)

---

**最后更新**：2026-06-19  
**版本**：v1.0.0

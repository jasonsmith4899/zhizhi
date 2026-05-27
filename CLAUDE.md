# CLAUDE.md - 智知 AI 知识库项目

## 项目概述

**智知 (ZhiZhi)** - 面向中小企业的多租户AI知识库SaaS产品。上传文档，即可获得智能问答机器人，通过微信小程序直接服务终端客户。

### 核心特性
- 多租户架构：共享数据库 + tenant_id 隔离
- 智能问答：基于RAG架构，从知识库检索相关信息并生成精准回答
- 多知识库：支持创建多个知识库，按业务场景分类管理
- 文档管理：支持PDF/TXT/MD格式文档上传，自动解析切片向量化
- 多轮对话：支持上下文记忆的连续对话
- 来源引用：回答中标注信息来源，可追溯可验证
- 三端合一：一套后端API服务商户管理后台(Vue3) + 微信小程序 + 平台管理

## 技术栈

| 层次 | 技术 | 说明 |
|------|------|------|
| 后端框架 | Spring Boot 3.4 | Java 17 |
| AI框架 | Spring AI 1.0.0 GA | 智谱GLM-4 |
| 向量存储 | PostgreSQL + pgvector | 业务数据+向量一体化 |
| 文档解析 | Apache Tika | 支持PDF/Word/TXT/MD |
| 商户管理 | Vue 3 + Element Plus + Pinia | 响应式SPA |
| 终端用户 | 微信小程序 | 原生开发 |
| 部署 | Docker Compose | 一键启动 |

## 项目结构

```
zhizhi/
├── backend/                    # Spring Boot后端
│   ├── src/main/java/com/zhizhi/ai/
│   │   ├── config/             # 配置（AI、安全、Web、租户拦截器）
│   │   ├── controller/         # REST API
│   │   │   ├── AuthController      # 认证
│   │   │   ├── KnowledgeController # 知识库管理
│   │   │   ├── DocumentController  # 文档管理
│   │   │   ├── ChatController      # 对话
│   │   │   ├── TenantController    # 租户管理
│   │   │   ├── MpController        # 小程序专用API
│   │   │   └── HealthController    # 健康检查
│   │   ├── service/            # 业务逻辑
│   │   ├── model/              # 实体、DTO
│   │   ├── repository/         # 数据访问
│   │   └── common/             # 通用工具(JWT/TenantContext/异常)
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/       # Flyway迁移脚本
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                   # Vue 3 商户管理后台
│   ├── src/
│   │   ├── views/              # 页面(登录/仪表盘/知识库/对话/设置)
│   │   ├── api/                # API请求封装
│   │   ├── stores/             # Pinia状态管理
│   │   └── router/             # Vue Router
│   ├── Dockerfile
│   └── nginx.conf
├── miniprogram/                # 微信小程序
│   ├── pages/
│   │   ├── index/              # 首页(知识库列表)
│   │   ├── chat/               # 聊天界面
│   │   ├── history/            # 对话历史
│   │   └── profile/            # 个人中心
│   └── utils/                  # 工具(request/auth)
├── docker-compose.yml
├── docker-compose.server.yml
├── config/                     # 配置文件
└── README.md
```

## 关键命令

### 开发环境启动

```bash
# 启动数据库
docker-compose up -d postgres

# 配置环境变量
export ZHIPU_API_KEY="your-api-key-here"
export DEEPSEEK_API_KEY="your-deepseek-key"
export SILICONFLOW_API_KEY="your-siliconflow-key"
export JWT_SECRET="zhizhi-ai-knowledge-base-jwt-secret-key-2026"

# 启动后端
cd backend
mvn spring-boot:run

# 启动前端
cd frontend
npm install
npm run dev
```

### Docker部署

```bash
# 一键部署
docker-compose up -d

# 查看日志
docker logs -f zhizhi-backend

# 重启服务
docker-compose restart
```

### API测试

```bash
# 登录获取token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test123456"}'

# 上传文档
curl -X POST "http://localhost:8080/api/v1/documents/upload" \
  -H "Authorization: Bearer <token>" \
  -F "file=@document.pdf" \
  -F "knowledgeBaseId=1"

# 智能问答
curl -X POST "http://localhost:8080/api/v1/chat" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"message":"如何退货？","knowledgeBaseId":1}'
```

## 代码规范

### 包结构
- `com.zhizhi.ai.config` - 配置类
- `com.zhizhi.ai.controller` - REST控制器
- `com.zhizhi.ai.service` - 业务逻辑
- `com.zhizhi.ai.model` - 实体和DTO
- `com.zhizhi.ai.repository` - 数据访问层
- `com.zhizhi.ai.common` - 通用工具

### 多租户隔离
- 使用 `TenantContext` 线程变量存储当前租户ID
- 通过 `TenantInterceptor` 自动从JWT中提取tenant_id
- 所有数据查询必须包含 `WHERE tenant_id = ?` 条件

### API规范
- RESTful风格：`/api/v1/{resource}`
- 认证：JWT Bearer Token
- 响应格式：`{"code": 200, "message": "success", "data": {...}}`
- 错误处理：统一异常处理，返回标准错误格式

### 数据库
- PostgreSQL + pgvector扩展
- Flyway迁移脚本在 `src/main/resources/db/migration/`
- 向量维度：1024（BGE-M3模型，见application.yml `spring.ai.vectorstore.pgvector.dimensions`）
- 向量表名：`document_chunks`（见application.yml `spring.ai.vectorstore.pgvector.table-name`）
- 元数据使用snake_case命名

### API接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/auth/register` | POST | 用户注册 |
| `/api/v1/auth/login` | POST | 用户登录，返回accessToken + refreshToken |
| `/api/v1/auth/refresh` | POST | 刷新token，body: `{"refreshToken": "..."}` |
| `/api/v1/auth/me` | GET | 获取当前用户信息 |
| `/api/v1/auth/api-key` | GET | 获取当前API Key |
| `/api/v1/auth/api-key` | POST | 生成/重新生成API Key |
| `/api/v1/auth/api-key` | DELETE | 吊销API Key |
| `/api/v1/knowledge-bases` | CRUD | 知识库管理 |
| `/api/v1/documents/upload` | POST | 上传文档 |
| `/api/v1/chat` | POST | 智能问答（同步） |
| `/api/v1/chat/stream` | POST | 智能问答（SSE流式返回） |
| `/api/v1/chat/conversations` | GET | 对话历史列表 |
| `/api/v1/chat/conversations/{id}/messages` | GET | 对话消息详情 |
| `/api/v1/mp/**` | - | 微信小程序专用接口 |

认证方式：
- JWT Bearer Token：`Authorization: Bearer <token>`
- API Key：`Authorization: Api-Key <apiKey>`（仅部分接口支持）

## 常见陷阱

1. **向量维度不匹配**：确保embedding模型输出维度与数据库向量列维度一致（当前1024，见application.yml）
2. **租户数据泄露**：所有查询必须包含tenant_id过滤，否则会导致跨租户数据访问
3. **Token过期处理**：前端需要处理401响应，自动跳转登录页
4. **文档解析失败**：Tika可能无法解析某些格式，需要提供错误提示
5. **内存不足**：大文档处理时注意JVM内存配置，建议 `-Xms256m -Xmx512m`
6. **embedding URL配置**：Spring AI的`OpenAiApi.builder().baseUrl()`会自动追加`/v1`，配置时不要包含`/v1`后缀

## 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| DEEPSEEK_API_KEY | DeepSeek API密钥 | 必填 |
| SILICONFLOW_API_KEY | SiliconFlow API密钥（用于embedding） | 必填 |
| JWT_SECRET | JWT签名密钥 | `zhizhi-ai-knowledge-base-jwt-secret-key-2026` |
| DB_HOST | 数据库主机 | `localhost` |
| DB_PORT | 数据库端口 | `5432` |
| DB_NAME | 数据库名 | `zhizhi` |
| DB_USER | 数据库用户 | `postgres` |
| DB_PASSWORD | 数据库密码 | `Zhizhi@2026#PgSql` |

## 访问地址

- 商户管理后台: http://localhost:3000
- API: http://localhost:8080/api/v1
- 健康检查: http://localhost:8080/api/v1/health

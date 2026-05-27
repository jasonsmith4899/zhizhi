# 智知 - AI知识库 (ZhiZhi AI Knowledge Base)

面向中小企业的多租户AI知识库SaaS产品。上传文档，即可获得智能问答机器人，通过微信小程序直接服务终端客户。

## 核心特性

- **多租户架构** - 共享数据库 + tenant_id 隔离，一套服务多个商户
- **智能问答** - 基于RAG架构，从知识库检索相关信息并生成精准回答
- **多知识库** - 支持创建多个知识库，按业务场景分类管理
- **文档管理** - 支持PDF/TXT/MD格式文档上传，自动解析切片向量化
- **多轮对话** - 支持上下文记忆的连续对话
- **来源引用** - 回答中标注信息来源，可追溯可验证
- **三端合一** - 一套后端API服务商户管理后台(Vue3) + 微信小程序 + 平台管理

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
zhizhi-ai-knowledge-base/
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
└── README.md
```

## 快速开始

### 前置条件

- JDK 17+
- Docker & Docker Compose
- [智谱AI API Key](https://open.bigmodel.cn/)

### 1. 启动数据库

```bash
docker-compose up -d postgres
```

### 2. 配置环境变量

```bash
export ZHIPU_API_KEY="your-api-key-here"
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 5. 访问

- 商户管理后台: http://localhost:3000
- API: http://localhost:8080/api/v1
- 健康检查: http://localhost:8080/api/v1/health

### Docker一键部署

```bash
cp .env.example .env
# 编辑 .env 填入API Key
docker-compose up -d
```

## API文档

### 认证

```bash
# 注册
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"123456"}'

# 登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'
```

### 知识库管理

```bash
# 创建知识库
curl -X POST http://localhost:8080/api/v1/knowledge-bases \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"产品文档","description":"产品使用手册"}'

# 上传文档
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@manual.pdf" \
  -F "knowledgeBaseId=1"
```

### 智能问答

```bash
# 普通对话
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"message":"如何退货？","knowledgeBaseId":1}'

# 多轮对话（传入sessionId）
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"message":"订单号是12345","sessionId":"abc123","knowledgeBaseId":1}'
```

### 租户管理

```bash
# 创建租户
curl -X POST http://localhost:8080/api/v1/tenants \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"示例商户"}'

# 查看我的租户
curl -X GET http://localhost:8080/api/v1/tenants/mine \
  -H "Authorization: Bearer <token>"
```

## 变现模式

| 套餐 | 价格 | 文档数 | 日问答量 |
|------|------|--------|----------|
| 免费版 | ¥0 | 10篇 | 100次 |
| 基础版 | ¥99/月 | 100篇 | 1000次 |
| 专业版 | ¥299/月 | 500篇 | 无限 |
| 企业版 | 联系定价 | 不限 | 不限 |

## 部署指南

### 推荐配置

| 资源 | 配置 | 月费 |
|------|------|------|
| 云服务器 | 2核4G ECS | ¥80-120 |
| PostgreSQL | 自建Docker | ¥0 |
| 域名+SSL | Let's Encrypt | ¥7 |
| LLM | 智谱GLM-4-Flash | ¥0(免费额度) |
| **月总计** | | **¥87-127** |

### 微信小程序配置

1. 注册企业主体微信小程序（¥300/年）
2. 在商户后台配置 AppID 和 Secret
3. 修改 `miniprogram/project.config.json` 中的 `appid` 为你的小程序真实 AppID
3. 使用微信开发者工具打开 `miniprogram/` 目录
4. 修改 `app.js` 中的 `baseUrl` 为实际域名
5. 上传审核

## License

MIT License

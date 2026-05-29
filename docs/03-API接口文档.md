# 智知 AI知识库 - API接口文档

## 基础信息

- 基础路径: `/api/v1`
- 认证方式: JWT Bearer Token 或 API Key
- 响应格式: `{"code": 200, "message": "success", "data": {...}}`

## 1. 认证接口 (/api/v1/auth)

### POST /auth/register - 用户注册
请求: `{"username": "string", "email": "string", "password": "string"}`
响应: `{token, refreshToken, user: {id, username, email, plan}}`

### POST /auth/login - 用户登录
请求: `{"username": "string", "password": "string"}`
响应: `{token, refreshToken, user: {id, username, email, plan, apiKey}}`

### POST /auth/refresh - 刷新Token
请求: `{"refreshToken": "string"}`
限流: 5次/分钟
响应: `{token, refreshToken}`

### GET /auth/me - 获取当前用户
认证: 必需
响应: `{id, username, email, plan, dailyQueriesUsed}`

### POST /auth/api-key - 生成API Key
认证: 必需
响应: `{apiKey, apiKeyCreatedAt}`

### GET /auth/api-key - 查询API Key
认证: 必需
响应: `{apiKey, apiKeyCreatedAt}`

### DELETE /auth/api-key - 吊销API Key
认证: 必需

## 2. 知识库接口 (/api/v1/knowledge-bases)

### POST / - 创建知识库
请求: `{"name": "string", "description": "string", "systemPrompt": "string"}`
响应: KnowledgeBaseDTO

### GET / - 获取知识库列表
响应: `[KnowledgeBaseDTO]`

### GET /{id} - 获取知识库详情
响应: KnowledgeBaseDTO

### PUT /{id} - 更新知识库
请求: `{"name": "string", "description": "string", "systemPrompt": "string"}`
响应: KnowledgeBaseDTO

### DELETE /{id} - 删除知识库
说明: 级联删除关联的文档、切片、向量、对话

## 3. 文档接口 (/api/v1/documents)

### POST /upload - 上传文档
Content-Type: multipart/form-data
参数: file(文件), knowledgeBaseId(知识库ID)
说明: 异步处理(解析→切片→向量化)

### GET / - 获取文档列表
参数: knowledgeBaseId(必填)
响应: `[DocumentListDTO]` (不含content字段)

### GET /{id}/chunks - 获取文档切片
响应: `[DocumentChunk]`

### GET /{id}/vector-status - 向量状态
响应: `{status, chunkCount, vectorizedCount}`

### POST /{id}/re-vectorize - 重新向量化
说明: 删除旧向量，重新处理文档

### POST /batch-delete - 批量删除
请求: `{"documentIds": [1, 2, 3]}`

### GET /{id}/preview - 文档预览
响应: `{filename, content, chunkCount}`

### GET /{id}/download - 文档下载
响应: 文件流

### DELETE /{id} - 删除文档
说明: 级联删除切片和向量

## 4. 对话接口 (/api/v1/chat)

### POST / - 同步对话
请求: `{"message": "string", "knowledgeBaseId": 1, "sessionId": "string"}`
响应: ChatResponse `{reply, sessionId, messageId, sources}`
限流: 10次/分钟

### POST /stream - SSE流式对话
请求: 同上
响应: SSE事件流
- event: session → `{sessionId}`
- event: chunk → 文本片段
- event: sources → `[{documentId, filename, chunkContent, similarity}]`
- event: done → 完成

### GET /conversations - 对话历史列表
响应: `[ConversationDTO]`

### GET /conversations/{id}/messages - 对话消息
响应: `[MessageDTO]`

### DELETE /conversations/{id} - 删除对话

## 5. 租户接口 (/api/v1/tenants)

### POST / - 创建租户
请求: `{"name": "string"}`

### GET /mine - 我的租户

### GET /{id} - 租户详情

### PUT /{id} - 更新租户
请求: TenantUpdateRequest

### POST /{id}/members - 添加成员
请求: `{"username": "string", "role": "string"}`

### GET /{id}/members - 成员列表

### GET /{id}/dashboard - 租户仪表盘

## 6. 小程序接口 (/api/mp)

### POST /login - 微信登录
请求: `{"code": "微信授权码"}`

### POST /chat - 小程序对话
说明: 同/chat接口，自动创建会话

### GET /conversations - 对话列表

### GET /conversations/{id}/messages - 消息详情

### DELETE /conversations/{id} - 删除对话

## 7. 健康检查

### GET /api/v1/health
响应: `{service, status, version}`
认证: 无需

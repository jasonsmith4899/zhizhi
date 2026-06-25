#!/bin/bash
# 全接口真实调用扫描脚本（在服务器本地对 localhost:8080 执行）
# 覆盖：正常路径（带 token + 真实资源）+ 异常路径（无 token / 不存在 id / 坏参数 / 缺字段）
BASE="http://localhost:8080"
TOKEN=$(cat /tmp/sweep_token.txt)
AUTH="Authorization: Bearer $TOKEN"
JSON="Content-Type: application/json"
PASS=0; FAIL=0

# 调用并打印：方法 路径 -> HTTP状态码 + 响应前80字符
call() {
  local desc="$1"; local method="$2"; local path="$3"; shift 3
  local out code body
  out=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE$path" "$@" 2>&1)
  code=$(echo "$out" | tail -1)
  body=$(echo "$out" | head -n -1 | tr -d '\n' | head -c 90)
  printf "[%s] %-6s %-45s -> %s | %s\n" "$desc" "$method" "$path" "$code" "$body"
}

echo "######## A. 公开接口 ########"
call "正常" GET  "/api/v1/health"
call "正常" POST "/api/v1/auth/login"   -H "$JSON" -d '{"username":"mason","password":"Rj@4899520"}'
call "错误-密码错" POST "/api/v1/auth/login" -H "$JSON" -d '{"username":"mason","password":"WRONG"}'
call "错误-缺字段" POST "/api/v1/auth/login" -H "$JSON" -d '{"username":"mason"}'
call "错误-坏JSON" POST "/api/v1/auth/login" -H "$JSON" -d '{bad json'
call "错误-用户名占用" POST "/api/v1/auth/register" -H "$JSON" -d '{"username":"mason","email":"x@y.com","password":"Test12345"}'
call "错误-刷新无效token" POST "/api/v1/auth/refresh" -H "$JSON" -d '{"refreshToken":"invalid.token.here"}'

echo "######## B. 认证接口-正常（带 token）########"
call "正常" GET "/api/v1/auth/me"                 -H "$AUTH"
call "正常" GET "/api/v1/knowledge-bases"          -H "$AUTH"
call "正常" GET "/api/v1/knowledge-bases/1"        -H "$AUTH"
call "正常" GET "/api/v1/documents?knowledgeBaseId=1" -H "$AUTH"
call "正常" GET "/api/v1/categories?knowledgeBaseId=1" -H "$AUTH"
call "正常" GET "/api/v1/tags?knowledgeBaseId=1"   -H "$AUTH"
call "正常" GET "/api/v1/api-keys"                 -H "$AUTH"
call "正常" GET "/api/v1/audit-logs?page=0&size=5" -H "$AUTH"
call "正常" GET "/api/v1/chat/conversations"       -H "$AUTH"
call "正常" GET "/api/v1/tenants/mine"             -H "$AUTH"

echo "######## C. 认证接口-未授权（无 token，预期 401/403）########"
call "错误-无token" GET "/api/v1/knowledge-bases"
call "错误-无token" GET "/api/v1/auth/me"
call "错误-坏token" GET "/api/v1/auth/me"          -H "Authorization: Bearer garbage.token.xxx"

echo "######## D. 资源不存在（预期 404/403）########"
call "错误-KB不存在" GET "/api/v1/knowledge-bases/999999" -H "$AUTH"
call "错误-文档不存在" DELETE "/api/v1/documents/999999"   -H "$AUTH"
call "错误-会话不存在" GET "/api/v1/chat/conversations/999999/messages" -H "$AUTH"
call "错误-租户不存在" GET "/api/v1/tenants/999999"        -H "$AUTH"
call "错误-租户dashboard无权" GET "/api/v1/tenants/999999/dashboard" -H "$AUTH"

echo "######## E. 知识图谱接口 ########"
call "正常" GET "/api/v1/knowledge-bases/1/kg/stats"      -H "$AUTH"
call "正常" GET "/api/v1/knowledge-bases/1/kg/entities?page=0&size=5" -H "$AUTH"
call "正常" GET "/api/v1/knowledge-bases/1/kg/relations?page=0&size=5" -H "$AUTH"
call "正常" GET "/api/v1/knowledge-bases/1/kg/graph"      -H "$AUTH"
call "正常" GET "/api/v1/knowledge-bases/1/kg/search?q=test" -H "$AUTH"
call "错误-超大分页" GET "/api/v1/knowledge-bases/1/kg/entities?page=0&size=999999" -H "$AUTH"
call "错误-KB不存在" GET "/api/v1/knowledge-bases/999999/kg/stats" -H "$AUTH"
call "错误-实体不存在" GET "/api/v1/knowledge-bases/1/kg/entities/999999" -H "$AUTH"

echo "######## F. 写操作（正常创建 + 错误参数）########"
call "正常-建分类" POST "/api/v1/categories" -H "$AUTH" -H "$JSON" -d '{"knowledgeBaseId":1,"name":"sweep测试分类"}'
call "正常-建标签" POST "/api/v1/tags" -H "$AUTH" -H "$JSON" -d '{"knowledgeBaseId":1,"name":"sweep测试标签"}'
call "错误-建分类缺字段" POST "/api/v1/categories" -H "$AUTH" -H "$JSON" -d '{}'
call "错误-建KB缺字段" POST "/api/v1/knowledge-bases" -H "$AUTH" -H "$JSON" -d '{}'
call "错误-删不存在分类" DELETE "/api/v1/categories/999999" -H "$AUTH"
call "错误-批量删空" POST "/api/v1/documents/batch-delete" -H "$AUTH" -H "$JSON" -d '{"ids":[]}'

echo "######## G. 小程序接口 ########"
call "错误-mp登录无code" POST "/api/mp/login" -H "$JSON" -d '{}'
call "错误-mp登录坏code" POST "/api/mp/login" -H "$JSON" -d '{"code":"invalid_code_xxx"}'

echo "######## H. 文档对话端点（应被日志切面排除）########"
call "对话-缺KB" POST "/api/v1/chat" -H "$AUTH" -H "$JSON" -d '{"message":"hello"}'

echo "######## DONE ########"

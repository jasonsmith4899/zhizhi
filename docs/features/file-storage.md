# Feature: 原始文件存储服务

## 现状

上传文件后只存储解析文本，原始二进制被丢弃：
- PDF 的排版、图片、表格全部丢失
- 解析失败后无法重试（只能用已存的 content 重新向量化）
- 下载功能返回的是切片拼接的文本，不是原始文件

## 目标

存储用户上传的原始文件，支持：
1. 文件下载（返回原始文件，保留排版）
2. 解析失败后可重试（读取原始文件重新解析）
3. 文件预览（仍用解析后的文本）

## 设计方案

### 存储方案：本地磁盘

```
/opt/zhizhi/storage/
└── {tenant_id}/
    └── {knowledge_base_id}/
        └── {document_id}_{filename}
```

- Docker volume 挂载：`./storage:/app/storage`
- 不上 OSS（MVP 阶段，后续可扩展）
- 文件按租户+知识库分目录，方便按租户清理

### 数据库变更

documents 表新增字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| file_path | VARCHAR(500) | 原始文件存储路径（相对路径） |
| file_size | BIGINT | 原始文件大小（已有，确认单位是字节） |

Flyway 迁移：`V8__add_file_storage.sql`

```sql
ALTER TABLE documents ADD COLUMN IF NOT EXISTS file_path VARCHAR(500);
```

### 后端改动

#### DocumentService.uploadDocument()

当前流程：
```
file.getBytes() → Tika解析 → document.content → 切片 → 向量化 → fileBytes 丢弃
```

改为：
```
file.getBytes() → 保存原始文件到磁盘 → document.file_path
                → Tika解析 → document.content → 切片 → 向量化
```

关键代码位置：`DocumentService.java` 第 93-105 行（uploadDocument 方法）

新增私有方法：
```java
private String saveFile(Long tenantId, Long knowledgeBaseId, Long documentId,
                        String filename, byte[] fileBytes) {
    String relativePath = tenantId + "/" + knowledgeBaseId + "/"
            + documentId + "_" + filename;
    Path fullPath = Path.of(storageRoot, relativePath);
    Files.createDirectories(fullPath.getParent());
    Files.write(fullPath, fileBytes);
    return relativePath;
}
```

配置项：
```yaml
app:
  storage:
    root: /app/storage    # Docker 容器内路径
```

#### DocumentService.getDocumentDownload() 

当前返回切片拼接文本，改为返回原始文件：

```java
public Map<String, Object> getDocumentDownload(Long documentId, Long userId) {
    Document doc = validateDocumentAccess(documentId, userId);
    if (doc.getFilePath() != null) {
        Path fullPath = Path.of(storageRoot, doc.getFilePath());
        byte[] fileBytes = Files.readAllBytes(fullPath);
        return Map.of(
            "filename", doc.getFilename(),
            "content", fileBytes,
            "contentType", getContentType(doc.getFileType())
        );
    }
    // 降级：无原始文件时返回拼接文本（兼容旧数据）
    // ... 现有逻辑
}
```

#### DocumentService.reVectorize()

当前用 `doc.getContent()` 重新切片，改为读取原始文件重新解析：

```java
if (doc.getFilePath() != null) {
    Path fullPath = Path.of(storageRoot, doc.getFilePath());
    byte[] fileBytes = Files.readAllBytes(fullPath);
    self.processDocumentAsync(documentId, fileBytes, filename, kbId);
} else {
    // 降级：用已存 content（旧数据兼容）
    // ... 现有逻辑
}
```

#### DocumentService.deleteDocument()

删除文档时同步删除原始文件：

```java
if (doc.getFilePath() != null) {
    try {
        Files.deleteIfExists(Path.of(storageRoot, doc.getFilePath()));
    } catch (IOException e) {
        log.warn("删除原始文件失败: {}", doc.getFilePath(), e);
    }
}
```

### Docker Compose 变更

```yaml
backend:
  volumes:
    - ./config:/app/config:ro
    - ./storage:/app/storage    # 新增：原始文件存储
```

### 前端改动

**无需改动**。下载接口 `/documents/{id}/download` 已经用 `responseType: 'blob'`，后端返回原始文件二进制即可。

### 风险点

| 风险 | 应对 |
|------|------|
| 磁盘空间 | 小企业场景，文档量有限；后续可扩展 OSS |
| 文件名冲突 | 用 `{document_id}_{filename}` 命名，不会冲突 |
| 旧数据无原始文件 | 下载和重新向量化时降级到现有逻辑（拼接文本） |
| 大文件 | 已有 20MB 限制（`spring.servlet.multipart.max-file-size`） |
| 安全 | 文件路径由后端拼接，不暴露给前端，防止路径遍历 |

### 实施步骤

1. V8 迁移脚本：documents 表加 file_path
2. 后端：uploadDocument 加文件保存
3. 后端：download 改为返回原始文件
4. 后端：reVectorize 改为读取原始文件
5. 后端：deleteDocument 加文件清理
6. docker-compose.yml 加 storage volume
7. 旧数据兼容测试（无 file_path 的文档）

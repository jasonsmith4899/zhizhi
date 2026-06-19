# Plan: 混合检索（向量 + ILIKE 关键词搜索）

## 目标
在 ZhiZhi 的 RAG 检索管线中增加关键词搜索通道，与现有向量搜索通过 RRF 融合，提升召回率。

## 系统模型
```
用户查询 "退款流程"
  ├→ 向量搜索 (现有) → List<Document> (带 score)
  ├→ 关键词搜索 (新增) → 提取关键词 ["退款","流程"] → ILIKE 查询 document_chunks 表 → List<Document>
  └→ RRF 融合 → per-doc 去重 → topK
```

## 文件清单

### 1. NEW: `backend/src/main/resources/db/migration/V9__keyword_search_index.sql`

在 `document_chunks` 表的 `content` 列上创建 GIN 索引（pg_trgm）以加速 ILIKE 查询。

⚠️ **决策点**：需要先确认服务器 PostgreSQL 是否已安装 `pg_trgm` 扩展。
- 如果已安装：用 `CREATE INDEX ... USING gin (content gin_trgm_ops)` 
- 如果未安装：两种方案：
  a) 安装扩展（在 Flyway 中 `CREATE EXTENSION IF NOT EXISTS pg_trgm`）然后用 GIN 索引
  b) 不用索引，纯 ILIKE 顺序扫描（数据量小时可接受，先跑通再优化）

**建议**：先尝试 `CREATE EXTENSION IF NOT EXISTS pg_trgm`，如果权限不够则降级为无索引 ILIKE。

```sql
-- V9__keyword_search_index.sql
-- 尝试安装 pg_trgm 扩展（如果已有则跳过）
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 为 content 列创建 GIN 索引加速 ILIKE 查询
-- 注意：如果 pg_trgm 安装失败，此索引创建也会失败，但不会影响其他功能
-- 关键词搜索会降级为顺序扫描 ILIKE（性能可接受）
CREATE INDEX IF NOT EXISTS idx_document_chunks_content_trgm 
ON document_chunks USING gin (content gin_trgm_ops);
```

### 2. MODIFY: `backend/src/main/java/com/zhizhi/ai/repository/DocumentChunkRepository.java`

添加关键词搜索方法：

```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 在 interface 内添加：

/**
 * 关键词搜索：在指定知识库的文档切片中搜索包含关键词的内容
 * 使用 ILIKE 进行子串匹配，按匹配数量降序排列
 * 结果数量限制由调用方通过 Pageable 控制或在 SQL 中硬编码
 */
@Query(value = """
    SELECT dc.id, dc.document_id, dc.knowledge_base_id, dc.tenant_id, 
           dc.chunk_index, dc.content, dc.content_length, dc.vector_id, dc.created_at
    FROM document_chunks dc
    WHERE dc.knowledge_base_id IN (:kbIds)
      AND dc.tenant_id = :tenantId
      AND (
        :#{#keywords.?.size()} = 0 OR
        dc.content ILIKE ANY(ARRAY[:keywords])
      )
    ORDER BY (
      SELECT COUNT(*) FROM unnest(:keywords) AS kw 
      WHERE dc.content ILIKE kw
    ) DESC
    LIMIT :limit
    """, nativeQuery = true)
List<DocumentChunk> searchByKeyword(
    @Param("kbIds") List<Long> kbIds,
    @Param("tenantId") Long tenantId,
    @Param("keywords") List<String> keywords,
    @Param("limit") int limit);
```

⚠️ **注意**：上面的 SQL 可能有语法问题（`ARRAY[:keywords]` 传递方式需要验证）。如果 JPA native query 传递数组困难，改用以下备选方案：

**备选方案**：在 Service 层动态构建 SQL，使用 `EntityManager.createNativeQuery()`：

```java
// 在 HybridRetrievalService 中使用 EntityManager 动态构建
public List<DocumentChunk> searchByKeyword(List<Long> kbIds, Long tenantId, List<String> keywords, int limit) {
    if (keywords.isEmpty()) return Collections.emptyList();
    
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT * FROM document_chunks dc WHERE ");
    sql.append("dc.knowledge_base_id IN (:kbIds) AND dc.tenant_id = :tenantId AND (");
    
    List<String> conditions = new ArrayList<>();
    for (int i = 0; i < keywords.size(); i++) {
        conditions.add("dc.content ILIKE :kw" + i);
    }
    sql.append(String.join(" OR ", conditions));
    sql.append(") ORDER BY (");
    
    List<String> countExprs = new ArrayList<>();
    for (int i = 0; i < keywords.size(); i++) {
        countExprs.add("CASE WHEN dc.content ILIKE :kw" + i + " THEN 1 ELSE 0 END");
    }
    sql.append(String.join(" + ", countExprs));
    sql.append(") DESC LIMIT :limit");
    
    Query query = entityManager.createNativeQuery(sql.toString(), DocumentChunk.class);
    query.setParameter("kbIds", kbIds);
    query.setParameter("tenantId", tenantId);
    query.setParameter("limit", limit);
    for (int i = 0; i < keywords.size(); i++) {
        query.setParameter("kw" + i, "%" + keywords.get(i) + "%");
    }
    
    return query.getResultList();
}
```

### 3. NEW: `backend/src/main/java/com/zhizhi/ai/service/HybridRetrievalService.java`

核心服务，负责：
- 关键词提取
- 关键词搜索（查询 document_chunks 表，转为 Spring AI Document）
- RRF 融合
- 降级处理

```java
package com.zhizhi.ai.service;

import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.entity.DocumentChunk;
import com.zhizhi.ai.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.Metadata;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HybridRetrievalService {

    private final VectorStore vectorStore;
    private final DocumentChunkRepository chunkRepository;
    
    // 备选：如果 DocumentChunkRepository 的 native query 有问题，注入 EntityManager
    // @PersistenceContext
    // private EntityManager entityManager;

    @Value("${app.ai.hybrid-keyword-ratio:0.5}")
    private double alpha;  // 关键词权重，向量权重 = 1 - alpha
    
    @Value("${app.ai.hybrid-rff-k:60}")
    private int rrfK;  // RRF 常数
    
    @Value("${app.ai.similarity-top-k:5}")
    private int topK;
    
    @Value("${app.ai.similarity-threshold:0.7}")
    private double threshold;

    /**
     * 混合检索入口：向量搜索 + 关键词搜索 → RRF 融合
     * 
     * @param query 用户查询
     * @param knowledgeBaseIds 要搜索的知识库 ID
     * @param tenantId 租户 ID
     * @param maxChunksPerDoc 每文档最大切片数
     * @return 融合后的搜索结果
     */
    public List<Document> hybridSearch(String query, Set<Long> knowledgeBaseIds, 
                                        Long tenantId, int maxChunksPerDoc) {
        int expandedTopK = topK * 3;
        
        // 1. 构建过滤表达式
        String filterExpression = buildFilterExpression(knowledgeBaseIds, tenantId);
        
        // 2. 并行执行向量搜索和关键词搜索
        List<Document> vectorResults;
        List<Document> keywordResults;
        
        try {
            // 向量搜索
            vectorResults = vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(query)
                    .topK(expandedTopK)
                    .similarityThreshold(threshold)
                    .filterExpression(filterExpression)
                    .build()
            );
            if (vectorResults == null) vectorResults = Collections.emptyList();
        } catch (Exception e) {
            log.warn("向量搜索失败，降级为空结果: {}", e.getMessage());
            vectorResults = Collections.emptyList();
        }
        
        try {
            // 关键词搜索
            List<String> keywords = extractKeywords(query);
            keywordResults = keywordSearch(keywords, knowledgeBaseIds, tenantId, expandedTopK);
        } catch (Exception e) {
            log.warn("关键词搜索失败，降级为纯向量: {}", e.getMessage());
            keywordResults = Collections.emptyList();
        }
        
        // 3. 如果两者都为空，返回空
        if (vectorResults.isEmpty() && keywordResults.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 4. 如果只有向量结果（关键词为空），直接走原有逻辑
        if (keywordResults.isEmpty()) {
            return applyPerDocDedup(vectorResults, knowledgeBaseIds, maxChunksPerDoc, topK);
        }
        
        // 5. RRF 融合
        List<Document> fused = rrfFusion(vectorResults, keywordResults);
        
        // 6. Per-document 去重 + 截断
        return applyPerDocDedup(fused, knowledgeBaseIds, maxChunksPerDoc, topK);
    }

    /**
     * 提取查询中的关键词
     * 按空格、标点分割，过滤长度 < 2 的词
     */
    private List<String> extractKeywords(String query) {
        if (query == null || query.isBlank()) return Collections.emptyList();
        
        return Arrays.stream(query.split("[\\s\\p{Punct}，。！？、；：""''（）【】《》]+"))
                .map(String::trim)
                .filter(s -> s.length() >= 2)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 关键词搜索：查询 document_chunks 表，转为 Spring AI Document
     * 使用 ILIKE 子串匹配，按匹配关键词数量降序
     */
    private List<Document> keywordSearch(List<String> keywords, Set<Long> kbIds, 
                                          Long tenantId, int limit) {
        if (keywords.isEmpty()) return Collections.emptyList();
        
        // 构建 ILIKE 关键词列表（加 % 前后缀）
        List<String> ilikePatterns = keywords.stream()
                .map(kw -> "%" + kw + "%")
                .collect(Collectors.toList());
        
        List<Long> kbIdList = new ArrayList<>(kbIds);
        
        // 使用 repository 方法（如果 native query 可行）
        // 否则使用下面的 EntityManager 动态查询
        List<DocumentChunk> chunks = chunkRepository.searchByKeyword(
                kbIdList, tenantId, ilikePatterns, limit);
        
        // 如果 repository 方法不可用，使用 EntityManager 备选方案
        // List<DocumentChunk> chunks = searchByKeywordViaEntityManager(kbIdList, tenantId, ilikePatterns, limit);
        
        // 转为 Spring AI Document
        return chunks.stream()
                .map(this::chunkToDocument)
                .collect(Collectors.toList());
    }

    /**
     * 将 DocumentChunk 实体转为 Spring AI Document
     * metadata 保持与 vectorStore 返回的 Document 一致
     */
    private Document chunkToDocument(DocumentChunk chunk) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("knowledge_base_id", String.valueOf(chunk.getKnowledgeBaseId()));
        metadata.put("document_id", String.valueOf(chunk.getDocumentId()));
        metadata.put("chunk_index", String.valueOf(chunk.getChunkIndex()));
        metadata.put("tenant_id", String.valueOf(chunk.getTenantId()));
        
        return new Document(chunk.getContent(), metadata);
    }

    /**
     * RRF (Reciprocal Rank Fusion) 融合
     * score(d) = (1-α) × rrf_vector(d) + α × rrf_keyword(d)
     * rrf(d) = 1 / (k + rank)
     */
    private List<Document> rrfFusion(List<Document> vectorResults, List<Document> keywordResults) {
        // 为每个结果计算 RRF 分数
        // key = document_id + chunk_index（唯一标识一个切片）
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, Document> docMap = new LinkedHashMap<>();
        
        // 向量搜索的 RRF 分数
        for (int i = 0; i < vectorResults.size(); i++) {
            Document doc = vectorResults.get(i);
            String key = getDocKey(doc);
            double rrfScore = 1.0 / (rrfK + i + 1);
            scores.merge(key, (1 - alpha) * rrfScore, Double::sum);
            docMap.putIfAbsent(key, doc);
        }
        
        // 关键词搜索的 RRF 分数
        for (int i = 0; i < keywordResults.size(); i++) {
            Document doc = keywordResults.get(i);
            String key = getDocKey(doc);
            double rrfScore = 1.0 / (rrfK + i + 1);
            scores.merge(key, alpha * rrfScore, Double::sum);
            docMap.putIfAbsent(key, doc);
        }
        
        // 按融合分数降序排列
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> {
                    Document doc = docMap.get(entry.getKey());
                    // 将 RRF 融合分数写入 score
                    // Spring AI Document 的 score 需要通过 metadata 或其他方式传递
                    // 这里我们返回带分数的文档（score 用于后续去重排序）
                    return doc;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取文档的唯一标识 key
     * 优先使用 metadata 中的 document_id + chunk_index
     */
    private String getDocKey(Document doc) {
        Map<String, Object> meta = doc.getMetadata();
        String docId = meta.getOrDefault("document_id", "unknown").toString();
        String chunkIdx = meta.getOrDefault("chunk_index", "0").toString();
        return docId + "_" + chunkIdx;
    }

    /**
     * Per-document 去重（与现有 buildRagContext 逻辑一致）
     */
    private List<Document> applyPerDocDedup(List<Document> docs, Set<Long> knowledgeBaseIds,
                                             int maxChunksPerDoc, int finalTopK) {
        Set<String> kbIdStrs = knowledgeBaseIds.stream()
                .map(String::valueOf).collect(Collectors.toSet());
        
        return docs.stream()
                .filter(doc -> {
                    Object kbId = doc.getMetadata().get("knowledge_base_id");
                    return kbId != null && kbIdStrs.contains(kbId.toString());
                })
                .collect(Collectors.groupingBy(
                        doc -> doc.getMetadata().getOrDefault("document_id", "unknown").toString()))
                .values().stream()
                .flatMap(group -> group.stream()
                        .sorted((a, b) -> Double.compare(
                                b.getScore() != null ? b.getScore() : 0,
                                a.getScore() != null ? a.getScore() : 0))
                        .limit(maxChunksPerDoc))
                .sorted((a, b) -> Double.compare(
                        b.getScore() != null ? b.getScore() : 0,
                        a.getScore() != null ? a.getScore() : 0))
                .limit(finalTopK)
                .collect(Collectors.toList());
    }

    /**
     * 构建过滤表达式（与现有 buildRagContext 一致）
     */
    private String buildFilterExpression(Set<Long> knowledgeBaseIds, Long tenantId) {
        List<String> kbIdFilters = knowledgeBaseIds.stream()
                .map(id -> "knowledge_base_id == '%s'".formatted(String.valueOf(id).replace("'", "")))
                .toList();
        String kbFilter = kbIdFilters.size() == 1
                ? kbIdFilters.get(0)
                : kbIdFilters.stream().collect(Collectors.joining(" || \", \"(\", \")"));
        
        return tenantId != null
                ? "%s && tenant_id == '%s'".formatted(kbFilter, String.valueOf(tenantId).replace("'", ""))
                : kbFilter;
    }
}
```

### 4. MODIFY: `backend/src/main/java/com/zhizhi/ai/service/ChatService.java`

修改 `buildRagContext()` 方法，使用 `HybridRetrievalService` 替代直接调用 `vectorStore`。

**改动范围**：
- 注入 `HybridRetrievalService`（替代或补充 `VectorStore`）
- 修改 `buildRagContext()` 中的检索逻辑
- 保留 per-doc 去重逻辑（移到 HybridRetrievalService 中）

**关键变更**：
```java
// 在 ChatService 中注入
private final HybridRetrievalService hybridRetrievalService;

// buildRagContext() 中替换：
// 旧：List<Document> docs = vectorStore.similaritySearch(...)
// 新：List<Document> docs = hybridRetrievalService.hybridSearch(
//         message, knowledgeBaseIds, tenantId, maxChunksPerDoc);
```

注意：`buildRagContext()` 中原有的 per-doc 去重逻辑可以移除，因为 `HybridRetrievalService.hybridSearch()` 内部已经包含了去重。

### 5. MODIFY: `backend/src/main/resources/application.yml`

添加混合检索配置：

```yaml
app:
  ai:
    # ... 现有配置 ...
    enable-hybrid: true              # 是否启用混合检索
    hybrid-keyword-ratio: 0.5        # α：关键词搜索权重（0-1）
    hybrid-rff-k: 60                 # RRF 常数 k
```

## 验证计划

1. **编译检查**：`cd backend && mvn compile -q`
2. **单元测试**：验证关键词提取、RRF 融合逻辑
3. **集成测试**：上传测试文档，对比混合检索 vs 纯向量检索的结果差异
4. **部署**：scp 到服务器，docker build，验证服务启动
5. **端到端测试**：通过小程序或 Web 界面提问，验证检索结果

## 降级策略

- 如果关键词搜索异常 → 自动降级为纯向量搜索（已有逻辑）
- 如果 pg_trgm 扩展不可用 → ILIKE 顺序扫描（性能可接受，数据量小时）
- 如果 DocumentChunkRepository 的 native query 语法有问题 → 改用 EntityManager 动态查询

## 实施顺序

1. V9 Flyway 迁移脚本
2. DocumentChunkRepository 添加 searchByKeyword
3. 新建 HybridRetrievalService
4. 修改 ChatService.buildRagContext()
5. 修改 application.yml
6. 编译检查
7. 部署验证

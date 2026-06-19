package com.zhizhi.ai.service;

import com.zhizhi.ai.model.entity.DocumentChunk;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
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
    private final RerankerService rerankerService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.ai.hybrid-keyword-ratio:0.5}")
    private double alpha;

    @Value("${app.ai.hybrid-rff-k:60}")
    private int rrfK;

    @Value("${app.ai.similarity-top-k:5}")
    private int topK;

    @Value("${app.ai.similarity-threshold:0.7}")
    private double threshold;

    @Value("${app.ai.max-chunks-per-doc:2}")
    private int maxChunksPerDoc;

    // --- 公开方法 ---

    /**
     * 混合检索：向量搜索 + 全文搜索 + 关键词搜索 → RRF 融合 → per-doc 去重 → Rerank → topK
     */
    public List<Document> hybridSearch(String query, Set<Long> knowledgeBaseIds, Long tenantId) {
        // 1. 提取关键词
        List<String> keywords = extractKeywords(query);
        int expandedTopK = topK * 3;

        // 2. 向量搜索（现有逻辑）
        List<Document> vectorResults = vectorSearch(query, knowledgeBaseIds, tenantId, expandedTopK);

        // 3. 全文搜索（新增）
        List<Document> fullTextResults = fullTextSearch(query, knowledgeBaseIds, tenantId, expandedTopK);

        // 4. 关键词搜索
        List<Document> keywordResults = keywordSearch(keywords, knowledgeBaseIds, tenantId, expandedTopK);

        // 5. RRF 融合（三路融合）
        List<Document> fused = rrfFusion(vectorResults, fullTextResults, keywordResults);

        // 6. Per-doc 去重
        List<Document> dedupedCandidates = applyPerDocDedup(fused, knowledgeBaseIds);

        // 7. Rerank 排序（最多对topK*2个候选进行重排）
        List<Document> candidates = dedupedCandidates.stream()
                .limit((long) topK * 2)
                .collect(Collectors.toList());
        return rerankerService.rerank(query, candidates);
    }

    // --- 向量搜索（封装现有逻辑）---

    private List<Document> vectorSearch(String query, Set<Long> kbIds, Long tenantId, int expandedTopK) {
        try {
            String filterExpression = buildFilterExpression(kbIds, tenantId);
            List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(query)
                    .topK(expandedTopK)
                    .similarityThreshold(threshold)
                    .filterExpression(filterExpression)
                    .build()
            );
            return results != null ? results : Collections.emptyList();
        } catch (Exception e) {
            log.warn("向量搜索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // --- 全文搜索 ---

    @SuppressWarnings("unchecked")
    private List<Document> fullTextSearch(String query, Set<Long> kbIds, Long tenantId, int limit) {
        try {
            List<Long> kbIdList = new ArrayList<>(kbIds);

            // 使用 PostgreSQL tsvector 进行全文搜索
            String sql = """
                    SELECT dc.* FROM document_chunks dc
                    WHERE dc.knowledge_base_id IN (:kbIds)
                    AND dc.tenant_id = :tenantId
                    AND dc.content_tsvector @@ plainto_tsquery('chinese', :query)
                    ORDER BY ts_rank(dc.content_tsvector, plainto_tsquery('chinese', :query)) DESC
                    LIMIT :limit
                    """;

            Query nativeQuery = entityManager.createNativeQuery(sql, DocumentChunk.class);
            nativeQuery.setParameter("kbIds", kbIdList);
            nativeQuery.setParameter("tenantId", tenantId);
            nativeQuery.setParameter("query", query);
            nativeQuery.setParameter("limit", limit);

            List<DocumentChunk> chunks = nativeQuery.getResultList();

            // 转为 Spring AI Document
            return chunks.stream().map(chunk -> {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("knowledge_base_id", String.valueOf(chunk.getKnowledgeBaseId()));
                metadata.put("document_id", String.valueOf(chunk.getDocumentId()));
                metadata.put("chunk_index", String.valueOf(chunk.getChunkIndex()));
                metadata.put("tenant_id", String.valueOf(chunk.getTenantId()));
                return new Document(chunk.getContent(), metadata);
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("全文搜索失败，降级为纯向量: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // --- 关键词搜索 ---

    private List<String> extractKeywords(String query) {
        if (query == null || query.isBlank()) return Collections.emptyList();
        // 按空白和标点分词，过滤掉长度<2的词
        return Arrays.stream(query.split("[\\s\\p{Punct}\\uFF0C\\u3002\\uFF01\\uFF1F\\u3001\\uFF1B\\uFF1A\\u201C\\u201D\\u2018\\u2019\\uFF08\\uFF09\\u3010\\u3011\\u300A\\u300B]+"))
                .map(String::trim)
                .filter(s -> s.length() >= 2)
                .distinct()
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Document> keywordSearch(List<String> keywords, Set<Long> kbIds, Long tenantId, int limit) {
        if (keywords.isEmpty()) return Collections.emptyList();

        try {
            List<Long> kbIdList = new ArrayList<>(kbIds);

            // 动态构建 SQL
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT dc.* FROM document_chunks dc WHERE ");
            sql.append("dc.knowledge_base_id IN (:kbIds) AND dc.tenant_id = :tenantId AND (");

            List<String> orConditions = new ArrayList<>();
            for (int i = 0; i < keywords.size(); i++) {
                orConditions.add("dc.content ILIKE :kw" + i);
            }
            sql.append(String.join(" OR ", orConditions));
            sql.append(") ORDER BY (");

            List<String> countExprs = new ArrayList<>();
            for (int i = 0; i < keywords.size(); i++) {
                countExprs.add("CASE WHEN dc.content ILIKE :kw" + i + " THEN 1 ELSE 0 END");
            }
            sql.append(String.join(" + ", countExprs));
            sql.append(") DESC LIMIT :limit");

            Query query = entityManager.createNativeQuery(sql.toString(), DocumentChunk.class);
            query.setParameter("kbIds", kbIdList);
            query.setParameter("tenantId", tenantId);
            query.setParameter("limit", limit);
            for (int i = 0; i < keywords.size(); i++) {
                query.setParameter("kw" + i, "%" + keywords.get(i) + "%");
            }

            List<DocumentChunk> chunks = query.getResultList();

            // 转为 Spring AI Document
            return chunks.stream().map(chunk -> {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("knowledge_base_id", String.valueOf(chunk.getKnowledgeBaseId()));
                metadata.put("document_id", String.valueOf(chunk.getDocumentId()));
                metadata.put("chunk_index", String.valueOf(chunk.getChunkIndex()));
                metadata.put("tenant_id", String.valueOf(chunk.getTenantId()));
                return new Document(chunk.getContent(), metadata);
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("关键词搜索失败，降级为纯向量: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // --- RRF 融合 ---

    private List<Document> rrfFusion(List<Document> vectorResults,
                                     List<Document> fullTextResults,
                                     List<Document> keywordResults) {
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, Document> docMap = new LinkedHashMap<>();

        // 向量搜索 RRF 分数（权重0.33）
        for (int i = 0; i < vectorResults.size(); i++) {
            Document doc = vectorResults.get(i);
            String key = getDocKey(doc);
            double rrfScore = 1.0 / (rrfK + i + 1);
            scores.merge(key, 0.33 * rrfScore, Double::sum);
            docMap.putIfAbsent(key, doc);
        }

        // 全文搜索 RRF 分数（权重0.33）
        for (int i = 0; i < fullTextResults.size(); i++) {
            Document doc = fullTextResults.get(i);
            String key = getDocKey(doc);
            double rrfScore = 1.0 / (rrfK + i + 1);
            scores.merge(key, 0.33 * rrfScore, Double::sum);
            docMap.putIfAbsent(key, doc);
        }

        // 关键词搜索 RRF 分数（权重0.34）
        for (int i = 0; i < keywordResults.size(); i++) {
            Document doc = keywordResults.get(i);
            String key = getDocKey(doc);
            double rrfScore = 1.0 / (rrfK + i + 1);
            scores.merge(key, 0.34 * rrfScore, Double::sum);
            docMap.putIfAbsent(key, doc);
        }

        // 按融合分数降序
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> docMap.get(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String getDocKey(Document doc) {
        Map<String, Object> meta = doc.getMetadata();
        String docId = meta.getOrDefault("document_id", "unknown").toString();
        String chunkIdx = meta.getOrDefault("chunk_index", "0").toString();
        return docId + "_" + chunkIdx;
    }

    // --- Per-document 去重 ---

    private List<Document> applyPerDocDedup(List<Document> docs, Set<Long> knowledgeBaseIds) {
        Set<String> kbIdStrs = knowledgeBaseIds.stream().map(String::valueOf).collect(Collectors.toSet());

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
                .limit(topK)
                .collect(Collectors.toList());
    }

    // --- 过滤表达式 ---

    private String buildFilterExpression(Set<Long> knowledgeBaseIds, Long tenantId) {
        List<String> kbIdFilters = knowledgeBaseIds.stream()
                .map(id -> "knowledge_base_id == '%s'".formatted(String.valueOf(id).replace("'", "")))
                .toList();
        String kbFilter = kbIdFilters.size() == 1
                ? kbIdFilters.get(0)
                : kbIdFilters.stream().collect(Collectors.joining(" || ", "(", ")"));
        return tenantId != null
                ? "%s && tenant_id == '%s'".formatted(kbFilter, String.valueOf(tenantId).replace("'", ""))
                : kbFilter;
    }
}

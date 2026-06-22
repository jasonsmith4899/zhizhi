package com.zhizhi.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhizhi.ai.model.entity.KgEntity;
import com.zhizhi.ai.model.entity.KgRelation;
import com.zhizhi.ai.repository.KgEntityRepository;
import com.zhizhi.ai.repository.KgRelationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * KAG 轻量知识图谱服务。
 * 构建：LLM 抽取三元组 → 实体对齐去重 → 存入 PostgreSQL。
 * 查询：LLM 识别 query 实体 → 多跳子图遍历 → 序列化为文本供 RAG 注入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphService {

    private final ChatModel chatModel;
    private final KgEntityRepository entityRepository;
    private final KgRelationRepository relationRepository;

    @Value("${app.ai.kag.enabled:true}")
    private boolean enabled;

    @Value("${app.ai.kag.extract-confidence-threshold:0.6}")
    private double confidenceThreshold;

    @Value("${app.ai.kag.max-hops:2}")
    private int maxHops;

    @Value("${app.ai.kag.max-subgraph-triples:30}")
    private int maxSubgraphTriples;

    @Value("${app.ai.kag.max-extract-chars:6000}")
    private int maxExtractChars;

    private static final String DEFAULT_TYPE = "未知";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern JSON_ARRAY = Pattern.compile("\\[.*]", Pattern.DOTALL);

    // ==================== 构建 ====================

    /**
     * 从文档内容抽取三元组并存储。失败仅记日志，不影响主流程（图谱为增强项）。
     */
    public void extractAndStore(Long documentId, Long knowledgeBaseId, Long tenantId, String content) {
        if (!enabled || content == null || content.isBlank()) return;

        try {
            String text = content.length() > maxExtractChars
                    ? content.substring(0, maxExtractChars) : content;

            String raw = ChatClient.builder(chatModel)
                    .build()
                    .prompt()
                    .system(EXTRACT_SYSTEM_PROMPT)
                    .user(text)
                    .call()
                    .content();

            List<Triple> triples = parseTriples(raw);
            if (triples.isEmpty()) {
                log.info("KAG 未抽取到三元组: docId={}", documentId);
                return;
            }

            int stored = 0;
            for (Triple t : triples) {
                if (t.confidence() < confidenceThreshold) continue;
                if (t.source().isBlank() || t.target().isBlank() || t.predicate().isBlank()) continue;

                KgEntity source = upsertEntity(tenantId, knowledgeBaseId, t.source(), t.sourceType());
                KgEntity target = upsertEntity(tenantId, knowledgeBaseId, t.target(), t.targetType());
                if (source.getId().equals(target.getId())) continue;  // 跳过自环

                relationRepository.save(KgRelation.builder()
                        .tenantId(tenantId)
                        .knowledgeBaseId(knowledgeBaseId)
                        .sourceId(source.getId())
                        .targetId(target.getId())
                        .predicate(truncate(t.predicate(), 100))
                        .documentId(documentId)
                        .confidence((float) t.confidence())
                        .build());
                stored++;
            }
            log.info("KAG 图谱构建完成: docId={}, 抽取={}, 入库={}", documentId, triples.size(), stored);

        } catch (Exception e) {
            log.warn("KAG 抽取失败(不影响主流程): docId={}, err={}", documentId, e.getMessage());
        }
    }

    /** 实体对齐：存在则 mention_count++，否则新建 */
    private KgEntity upsertEntity(Long tenantId, Long kbId, String name, String type) {
        String t = (type == null || type.isBlank()) ? DEFAULT_TYPE : truncate(type, 50);
        String normName = normalize(name);
        return entityRepository
                .findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(tenantId, kbId, normName, t)
                .map(e -> {
                    e.setMentionCount(e.getMentionCount() + 1);
                    return entityRepository.save(e);
                })
                .orElseGet(() -> {
                    try {
                        return entityRepository.save(KgEntity.builder()
                                .tenantId(tenantId)
                                .knowledgeBaseId(kbId)
                                .name(truncate(name, 200))
                                .type(t)
                                .normName(normName)
                                .mentionCount(1)
                                .build());
                    } catch (Exception dup) {
                        // 并发下唯一约束冲突，回退查询
                        return entityRepository
                                .findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(tenantId, kbId, normName, t)
                                .orElseThrow(() -> dup instanceof RuntimeException re ? re : new RuntimeException(dup));
                    }
                });
    }

    // ==================== 查询 ====================

    /**
     * 根据 query 检索相关子图，序列化为文本。无命中返回 null。
     */
    public String querySubgraph(String query, Set<Long> knowledgeBaseIds, Long tenantId) {
        if (!enabled || query == null || query.isBlank() || knowledgeBaseIds.isEmpty()) return null;

        try {
            List<String> entityNames = recognizeEntities(query);
            if (entityNames.isEmpty()) return null;

            List<String> normNames = entityNames.stream()
                    .map(this::normalize).filter(s -> !s.isBlank()).distinct().toList();
            if (normNames.isEmpty()) return null;

            List<Long> kbIds = new ArrayList<>(knowledgeBaseIds);
            List<KgEntity> seeds = entityRepository
                    .findByTenantIdAndKnowledgeBaseIdInAndNormNameIn(tenantId, kbIds, normNames);
            if (seeds.isEmpty()) return null;

            List<Long> seedIds = seeds.stream().map(KgEntity::getId).toList();
            List<Object[]> rows = relationRepository.traverseSubgraph(
                    seedIds, tenantId, kbIds, maxHops, maxSubgraphTriples);
            if (rows.isEmpty()) return null;

            String subgraph = rows.stream()
                    .map(r -> "%s —%s→ %s".formatted(
                            String.valueOf(r[0]), String.valueOf(r[1]), String.valueOf(r[2])))
                    .distinct()
                    .collect(Collectors.joining("\n"));

            log.info("KAG 子图召回: seeds={}, triples={}", seedIds.size(), rows.size());
            return subgraph;

        } catch (Exception e) {
            log.warn("KAG 子图查询失败(降级为纯RAG): {}", e.getMessage());
            return null;
        }
    }

    /** 用 LLM 从 query 抽取实体名 */
    private List<String> recognizeEntities(String query) {
        try {
            String raw = ChatClient.builder(chatModel)
                    .build()
                    .prompt()
                    .system(RECOGNIZE_SYSTEM_PROMPT)
                    .user(query)
                    .call()
                    .content();

            Matcher m = JSON_ARRAY.matcher(raw);
            if (!m.find()) return Collections.emptyList();
            JsonNode arr = objectMapper.readTree(m.group());
            List<String> names = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    String name = n.asText("").trim();
                    if (!name.isBlank()) names.add(name);
                }
            }
            return names;
        } catch (Exception e) {
            log.warn("KAG query 实体识别失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 清理 ====================

    public void deleteByDocument(Long documentId) {
        try {
            relationRepository.deleteByDocumentId(documentId);
        } catch (Exception e) {
            log.warn("KAG 关系清理失败: docId={}, err={}", documentId, e.getMessage());
        }
    }

    public void deleteByKnowledgeBase(Long tenantId, Long knowledgeBaseId) {
        try {
            if (tenantId != null) {
                relationRepository.deleteByTenantIdAndKnowledgeBaseId(tenantId, knowledgeBaseId);
                entityRepository.deleteByTenantIdAndKnowledgeBaseId(tenantId, knowledgeBaseId);
            } else {
                relationRepository.deleteByKnowledgeBaseId(knowledgeBaseId);
                entityRepository.deleteByKnowledgeBaseId(knowledgeBaseId);
            }
        } catch (Exception e) {
            log.warn("KAG 知识库图谱清理失败: kbId={}, err={}", knowledgeBaseId, e.getMessage());
        }
    }

    // ==================== 辅助 ====================

    private record Triple(String source, String sourceType, String target,
                          String targetType, String predicate, double confidence) {}

    private List<Triple> parseTriples(String raw) {
        if (raw == null) return Collections.emptyList();
        Matcher m = JSON_ARRAY.matcher(raw);
        if (!m.find()) return Collections.emptyList();
        try {
            JsonNode arr = objectMapper.readTree(m.group());
            if (!arr.isArray()) return Collections.emptyList();
            List<Triple> result = new ArrayList<>();
            for (JsonNode n : arr) {
                String source = n.path("source").asText("").trim();
                String target = n.path("target").asText("").trim();
                String predicate = n.path("predicate").asText("").trim();
                if (source.isBlank() || target.isBlank() || predicate.isBlank()) continue;
                result.add(new Triple(
                        source,
                        n.path("sourceType").asText(DEFAULT_TYPE).trim(),
                        target,
                        n.path("targetType").asText(DEFAULT_TYPE).trim(),
                        predicate,
                        n.path("confidence").asDouble(1.0)
                ));
            }
            return result;
        } catch (Exception e) {
            log.warn("KAG 三元组解析失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String normalize(String name) {
        if (name == null) return "";
        return name.toLowerCase().replaceAll("\\s+", "");
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static final String EXTRACT_SYSTEM_PROMPT = """
            你是知识图谱抽取引擎。请从用户提供的文本中抽取实体之间的关系三元组。
            要求：
            1. 只抽取文本中明确表述的、有意义的实体关系，不要臆测或编造。
            2. 实体类型从：人物、组织、产品、概念、地点、时间、事件 中选择最贴切的一个。
            3. predicate 用简洁的中文动词或动词短语（如：负责、属于、包含、隶属于、开发、位于）。
            4. confidence 为 0~1 的小数，表示该关系在文本中的明确程度。
            5. 严格只输出 JSON 数组，不要任何解释文字、不要 markdown 代码块标记。
            输出格式：
            [{"source":"实体A","sourceType":"组织","target":"实体B","targetType":"人物","predicate":"负责","confidence":0.9}]
            若没有可抽取的关系，输出 []。
            """;

    private static final String RECOGNIZE_SYSTEM_PROMPT = """
            你是实体识别引擎。请从用户的问题中识别出涉及的关键实体名称（人物、组织、产品、概念等）。
            要求：
            1. 只输出实体名称本身，保持与问题中的表述一致。
            2. 严格只输出 JSON 字符串数组，不要任何解释、不要 markdown 代码块标记。
            输出格式：["实体A","实体B"]
            若无明确实体，输出 []。
            """;
}

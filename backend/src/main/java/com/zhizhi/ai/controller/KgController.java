package com.zhizhi.ai.controller;

import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.Result;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.dto.kg.*;
import com.zhizhi.ai.model.dto.kg.KgEntityDetailDTO.RelationInfo;
import com.zhizhi.ai.model.dto.kg.KgGraphDTO.GraphEdge;
import com.zhizhi.ai.model.dto.kg.KgGraphDTO.GraphNode;
import com.zhizhi.ai.model.dto.kg.KgStatsDTO.TopEntity;
import com.zhizhi.ai.model.dto.kg.KgStatsDTO.TypeCount;
import com.zhizhi.ai.model.entity.Document;
import com.zhizhi.ai.model.entity.KgEntity;
import com.zhizhi.ai.model.entity.KgRelation;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import com.zhizhi.ai.repository.DocumentRepository;
import com.zhizhi.ai.repository.KgEntityRepository;
import com.zhizhi.ai.repository.KgRelationRepository;
import com.zhizhi.ai.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识图谱管理接口
 */
@RestController
@RequestMapping("/api/v1/knowledge-bases/{kbId}/kg")
@RequiredArgsConstructor
public class KgController {

    private final KgEntityRepository entityRepository;
    private final KgRelationRepository relationRepository;
    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final AuthUtil authUtil;

    /**
     * 获取图谱统计数据：实体数、关系数、Top 实体、类型分布
     */
    @GetMapping("/stats")
    public Result<KgStatsDTO> stats(@PathVariable Long kbId, Authentication authentication) {
        Long tenantId = validateAndGetTenant(kbId, authentication);

        long entityCount = entityRepository.countByTenantIdAndKnowledgeBaseId(tenantId, kbId);
        long relationCount = relationRepository.countByTenantIdAndKnowledgeBaseId(tenantId, kbId);
        long totalMentions = entityRepository.sumMentionCountByTenantIdAndKnowledgeBaseId(tenantId, kbId);
        int avgMentionCount = entityCount > 0 ? (int) (totalMentions / entityCount) : 0;

        List<TopEntity> topEntities = entityRepository
                .findTop20ByTenantIdAndKnowledgeBaseIdOrderByMentionCountDesc(tenantId, kbId)
                .stream()
                .map(e -> new TopEntity(e.getName(), e.getType(), e.getMentionCount()))
                .toList();

        List<TypeCount> typeDistribution = entityRepository.countByType(tenantId, kbId)
                .stream()
                .map(row -> new TypeCount(
                        row[0] != null ? String.valueOf(row[0]) : "未知",
                        (Long) row[1]))
                .toList();

        return Result.ok(new KgStatsDTO(entityCount, relationCount, avgMentionCount, topEntities, typeDistribution));
    }

    /**
     * 分页查询实体列表（支持搜索和类型筛选）
     */
    @GetMapping("/entities")
    public Result<Page<KgEntityDTO>> entities(@PathVariable Long kbId,
                                               @RequestParam(required = false) String search,
                                               @RequestParam(required = false) String type,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               Authentication authentication) {
        Long tenantId = validateAndGetTenant(kbId, authentication);

        Page<KgEntity> entityPage = entityRepository.searchEntities(
                tenantId, kbId, search, type, PageRequest.of(Math.max(page, 0), clampSize(size)));

        Page<KgEntityDTO> dtoPage = entityPage.map(KgEntityDTO::fromEntity);
        return Result.ok(dtoPage);
    }

    /**
     * 分页查询关系列表（支持搜索和置信度筛选）
     */
    @GetMapping("/relations")
    public Result<Page<KgRelationDTO>> relations(@PathVariable Long kbId,
                                                  @RequestParam(required = false) String search,
                                                  @RequestParam(required = false) Float minConfidence,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  Authentication authentication) {
        Long tenantId = validateAndGetTenant(kbId, authentication);

        Page<KgRelation> relationPage = relationRepository.searchRelations(
                tenantId, kbId, search, minConfidence, PageRequest.of(Math.max(page, 0), clampSize(size)));

        // 批量预加载实体名称和文档名称，避免 N+1
        Set<Long> entityIds = new HashSet<>();
        Set<Long> documentIds = new HashSet<>();
        for (KgRelation r : relationPage.getContent()) {
            entityIds.add(r.getSourceId());
            entityIds.add(r.getTargetId());
            if (r.getDocumentId() != null) {
                documentIds.add(r.getDocumentId());
            }
        }

        Map<Long, String> entityNameMap = entityRepository.findAllById(entityIds).stream()
                .collect(Collectors.toMap(KgEntity::getId, KgEntity::getName));

        Map<Long, String> documentNameMap = documentIds.isEmpty() ? Map.of()
                : documentRepository.findAllById(documentIds).stream()
                        .collect(Collectors.toMap(Document::getId, Document::getFilename));

        Page<KgRelationDTO> dtoPage = relationPage.map(r -> {
            KgRelationDTO dto = new KgRelationDTO();
            dto.setId(r.getId());
            dto.setSourceName(entityNameMap.getOrDefault(r.getSourceId(), "未知"));
            dto.setTargetName(entityNameMap.getOrDefault(r.getTargetId(), "未知"));
            dto.setPredicate(r.getPredicate());
            dto.setConfidence(r.getConfidence());
            dto.setDocumentId(r.getDocumentId());
            dto.setDocumentName(r.getDocumentId() != null ? documentNameMap.get(r.getDocumentId()) : null);
            dto.setCreatedAt(r.getCreatedAt());
            return dto;
        });

        return Result.ok(dtoPage);
    }

    /**
     * 获取实体详情（含关联关系）
     */
    @GetMapping("/entities/{entityId}")
    public Result<KgEntityDetailDTO> entityDetail(@PathVariable Long kbId,
                                                   @PathVariable Long entityId,
                                                   Authentication authentication) {
        Long tenantId = validateAndGetTenant(kbId, authentication);

        KgEntity entity = entityRepository.findById(entityId)
                .orElseThrow(() -> BusinessException.notFound("实体"));

        // 验证实体归属
        if (!entity.getTenantId().equals(tenantId) || !entity.getKnowledgeBaseId().equals(kbId)) {
            throw BusinessException.forbidden("无权访问该实体");
        }

        // 查询关联关系（最多 50 条）
        List<KgRelation> relations = relationRepository.findByEntityId(
                tenantId, kbId, entityId, PageRequest.of(0, 50));

        // 收集关联实体 ID 并批量查询名称
        Set<Long> relatedEntityIds = new HashSet<>();
        for (KgRelation r : relations) {
            relatedEntityIds.add(r.getSourceId());
            relatedEntityIds.add(r.getTargetId());
        }
        Map<Long, String> entityNameMap = entityRepository.findAllById(relatedEntityIds).stream()
                .collect(Collectors.toMap(KgEntity::getId, KgEntity::getName));

        // 组装详情 DTO
        KgEntityDetailDTO detail = new KgEntityDetailDTO();
        detail.setId(entity.getId());
        detail.setName(entity.getName());
        detail.setType(entity.getType());
        detail.setDescription(entity.getDescription());
        detail.setMentionCount(entity.getMentionCount());
        detail.setCreatedAt(entity.getCreatedAt());

        List<RelationInfo> relationInfos = relations.stream().map(r -> {
            RelationInfo info = new RelationInfo();
            info.setId(r.getId());
            info.setPredicate(r.getPredicate());
            info.setConfidence(r.getConfidence());
            if (r.getSourceId().equals(entityId)) {
                info.setDirection("out");
                info.setOtherEntity(entityNameMap.getOrDefault(r.getTargetId(), "未知"));
            } else {
                info.setDirection("in");
                info.setOtherEntity(entityNameMap.getOrDefault(r.getSourceId(), "未知"));
            }
            return info;
        }).toList();

        detail.setRelations(relationInfos);
        return Result.ok(detail);
    }

    /**
     * 获取子图数据（用于前端可视化）
     *
     * 有 seedEntityId 时：从该实体出发，maxHops 跳内遍历
     * 无 seedEntityId 时：取 top maxNodes 个节点及关联边
     */
    @GetMapping("/graph")
    public Result<KgGraphDTO> graph(@PathVariable Long kbId,
                                     @RequestParam(required = false) Long seedEntityId,
                                     @RequestParam(defaultValue = "2") int maxHops,
                                     @RequestParam(defaultValue = "200") int maxNodes,
                                     Authentication authentication) {
        Long tenantId = validateAndGetTenant(kbId, authentication);

        // 钳制用户可控参数，避免在稠密图上无界遍历/查询放大
        maxHops = Math.max(1, Math.min(maxHops, MAX_HOPS_LIMIT));
        maxNodes = Math.max(1, Math.min(maxNodes, MAX_NODES_LIMIT));

        List<KgGraphDTO.GraphNode> nodes;
        List<KgGraphDTO.GraphEdge> edges;

        if (seedEntityId != null) {
            // 从种子实体出发遍历子图
            KgEntity seed = entityRepository.findById(seedEntityId)
                    .orElseThrow(() -> BusinessException.notFound("种子实体"));

            if (!seed.getTenantId().equals(tenantId) || !seed.getKnowledgeBaseId().equals(kbId)) {
                throw BusinessException.forbidden("无权访问该实体");
            }

            // 多跳遍历：逐层扩展节点集合，达到 maxNodes 即提前终止，避免稠密图节点膨胀
            Set<Long> nodeIds = new LinkedHashSet<>();
            nodeIds.add(seedEntityId);
            Set<Long> currentLevel = Set.of(seedEntityId);
            for (int hop = 0; hop < maxHops && !currentLevel.isEmpty() && nodeIds.size() < maxNodes; hop++) {
                List<KgRelation> levelRelations = relationRepository.findByNodeIds(
                        tenantId, kbId, new ArrayList<>(currentLevel));
                Set<Long> nextLevel = new HashSet<>();
                for (KgRelation r : levelRelations) {
                    nextLevel.add(r.getSourceId());
                    nextLevel.add(r.getTargetId());
                }
                nextLevel.removeAll(nodeIds);
                nodeIds.addAll(nextLevel);
                if (nodeIds.size() >= maxNodes) break;
                currentLevel = nextLevel;
            }

            // 限制节点数
            List<Long> limitedNodeIds = nodeIds.stream().limit(maxNodes).toList();

            // 查询这些节点间的所有关系
            List<KgRelation> allRelations = relationRepository.findByNodeIds(
                    tenantId, kbId, limitedNodeIds);

            // 构建节点 DTO
            Map<Long, KgEntity> entityMap = entityRepository.findAllById(limitedNodeIds).stream()
                    .collect(Collectors.toMap(KgEntity::getId, e -> e));
            nodes = limitedNodeIds.stream()
                    .filter(entityMap::containsKey)
                    .map(id -> {
                        KgEntity e = entityMap.get(id);
                        return new GraphNode(e.getId(), e.getName(), e.getType(), e.getMentionCount());
                    })
                    .toList();

            // 构建边 DTO
            edges = allRelations.stream()
                    .filter(r -> limitedNodeIds.contains(r.getSourceId()) && limitedNodeIds.contains(r.getTargetId()))
                    .map(r -> new GraphEdge(r.getSourceId(), r.getTargetId(), r.getPredicate(), r.getConfidence()))
                    .toList();

        } else {
            // 取 Top N 节点及关联边
            List<KgEntity> topEntities = entityRepository
                    .findTop20ByTenantIdAndKnowledgeBaseIdOrderByMentionCountDesc(tenantId, kbId)
                    .stream()
                    .limit(maxNodes)
                    .toList();

            List<Long> nodeIds = topEntities.stream().map(KgEntity::getId).toList();

            nodes = topEntities.stream()
                    .map(e -> new GraphNode(e.getId(), e.getName(), e.getType(), e.getMentionCount()))
                    .toList();

            List<KgRelation> allRelations = relationRepository.findByNodeIds(tenantId, kbId, nodeIds);
            edges = allRelations.stream()
                    .filter(r -> nodeIds.contains(r.getSourceId()) && nodeIds.contains(r.getTargetId()))
                    .map(r -> new GraphEdge(r.getSourceId(), r.getTargetId(), r.getPredicate(), r.getConfidence()))
                    .toList();
        }

        return Result.ok(new KgGraphDTO(nodes, edges));
    }

    /**
     * 模糊搜索实体
     */
    @GetMapping("/search")
    public Result<List<KgEntityDTO>> search(@PathVariable Long kbId,
                                             @RequestParam String q,
                                             @RequestParam(defaultValue = "10") int limit,
                                             Authentication authentication) {
        Long tenantId = validateAndGetTenant(kbId, authentication);

        List<KgEntity> entities = entityRepository.searchByName(
                tenantId, kbId, q, PageRequest.of(0, clampSize(limit)));

        return Result.ok(entities.stream().map(KgEntityDTO::fromEntity).toList());
    }

    // ==================== 辅助方法 ====================

    /** 分页/数量参数上限，防止单次请求拉取整库 */
    private static final int MAX_PAGE_SIZE = 100;
    /** 子图遍历跳数上限 */
    private static final int MAX_HOPS_LIMIT = 5;
    /** 子图节点数上限 */
    private static final int MAX_NODES_LIMIT = 500;

    /** 钳制分页大小到 [1, MAX_PAGE_SIZE] */
    private int clampSize(int size) {
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }

    /**
     * 验证租户并返回 tenantId，同时校验知识库归属
     */
    private Long validateAndGetTenant(Long kbId, Authentication authentication) {
        // 确保用户已认证（getUserId 在未认证时抛 401）
        Long userId = authUtil.getUserId(authentication);
        if (userId == null) {
            throw BusinessException.unauthorized("用户未认证");
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw BusinessException.unauthorized("租户信息缺失");
        }
        // 校验知识库归属当前租户
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> BusinessException.notFound("知识库"));
        if (!kb.getTenantId().equals(tenantId)) {
            throw BusinessException.forbidden("无权访问此知识库");
        }
        return tenantId;
    }
}

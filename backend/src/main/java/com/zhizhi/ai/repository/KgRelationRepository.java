package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.KgRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KgRelationRepository extends JpaRepository<KgRelation, Long> {

    /**
     * 多跳子图遍历（PostgreSQL 递归 CTE）。
     * 从种子实体出发无向遍历 maxHops 跳，收集涉及的关系三元组。
     * 返回：[sourceName, predicate, targetName, confidence]
     *
     * 终止性：递归项用 UNION（非 UNION ALL）对 (entity_id, hop) 去重，
     * 且 hop 单调递增并以 maxHops 为上界，故必然终止。
     */
    @Query(value = """
            WITH RECURSIVE nodes AS (
                SELECT id AS entity_id, 0 AS hop
                FROM kg_entities
                WHERE id IN (:seedIds)
              UNION
                SELECT CASE WHEN r.source_id = n.entity_id THEN r.target_id ELSE r.source_id END,
                       n.hop + 1
                FROM kg_relations r
                JOIN nodes n ON (r.source_id = n.entity_id OR r.target_id = n.entity_id)
                WHERE r.tenant_id = :tenantId
                  AND r.knowledge_base_id IN (:kbIds)
                  AND n.hop < :maxHops
            )
            SELECT DISTINCT se.name AS source_name,
                            gr.predicate AS predicate,
                            te.name AS target_name,
                            gr.confidence AS confidence
            FROM kg_relations gr
            JOIN nodes ns ON ns.entity_id = gr.source_id
            JOIN nodes nt ON nt.entity_id = gr.target_id
            JOIN kg_entities se ON se.id = gr.source_id
            JOIN kg_entities te ON te.id = gr.target_id
            WHERE gr.tenant_id = :tenantId
              AND gr.knowledge_base_id IN (:kbIds)
            ORDER BY gr.confidence DESC
            LIMIT :maxTriples
            """, nativeQuery = true)
    List<Object[]> traverseSubgraph(@Param("seedIds") List<Long> seedIds,
                                    @Param("tenantId") Long tenantId,
                                    @Param("kbIds") List<Long> kbIds,
                                    @Param("maxHops") int maxHops,
                                    @Param("maxTriples") int maxTriples);

    void deleteByDocumentId(Long documentId);

    void deleteByTenantIdAndKnowledgeBaseId(Long tenantId, Long knowledgeBaseId);

    void deleteByKnowledgeBaseId(Long knowledgeBaseId);
}

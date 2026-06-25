package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.KgEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KgEntityRepository extends JpaRepository<KgEntity, Long> {

    /** 实体对齐：按归一化名+类型定位已有实体 */
    Optional<KgEntity> findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(
            Long tenantId, Long knowledgeBaseId, String normName, String type);

    /** 问答阶段：用 query 中识别出的归一化名匹配候选实体 */
    List<KgEntity> findByTenantIdAndKnowledgeBaseIdInAndNormNameIn(
            Long tenantId, List<Long> knowledgeBaseIds, List<String> normNames);

    void deleteByTenantIdAndKnowledgeBaseId(Long tenantId, Long knowledgeBaseId);

    void deleteByKnowledgeBaseId(Long knowledgeBaseId);

    // ==================== Controller 查询方法 ====================

    /** 统计指定知识库的实体数量 */
    @Query("SELECT COUNT(e) FROM KgEntity e WHERE e.tenantId = :tenantId AND e.knowledgeBaseId = :kbId")
    long countByTenantIdAndKnowledgeBaseId(@Param("tenantId") Long tenantId, @Param("kbId") Long kbId);

    /** 统计指定知识库所有实体的提及次数总和 */
    @Query("SELECT COALESCE(SUM(e.mentionCount), 0) FROM KgEntity e WHERE e.tenantId = :tenantId AND e.knowledgeBaseId = :kbId")
    long sumMentionCountByTenantIdAndKnowledgeBaseId(@Param("tenantId") Long tenantId, @Param("kbId") Long kbId);

    /** 实体类型分布统计 */
    @Query("SELECT e.type, COUNT(e) FROM KgEntity e WHERE e.tenantId = :tenantId AND e.knowledgeBaseId = :kbId GROUP BY e.type ORDER BY COUNT(e) DESC")
    List<Object[]> countByType(@Param("tenantId") Long tenantId, @Param("kbId") Long kbId);

    /** 按提及次数取 Top 实体 */
    List<KgEntity> findTop20ByTenantIdAndKnowledgeBaseIdOrderByMentionCountDesc(Long tenantId, Long knowledgeBaseId);

    /** 分页查询实体（带搜索和类型筛选） */
    @Query("SELECT e FROM KgEntity e WHERE e.tenantId = :tenantId AND e.knowledgeBaseId = :kbId " +
           "AND (:search IS NULL OR e.name LIKE %:search%) " +
           "AND (:type IS NULL OR e.type = :type) " +
           "ORDER BY e.mentionCount DESC")
    Page<KgEntity> searchEntities(@Param("tenantId") Long tenantId, @Param("kbId") Long kbId,
                                   @Param("search") String search, @Param("type") String type,
                                   Pageable pageable);

    /** 模糊搜索实体（用于图谱搜索） */
    @Query("SELECT e FROM KgEntity e WHERE e.tenantId = :tenantId AND e.knowledgeBaseId = :kbId " +
           "AND e.name LIKE %:q% ORDER BY e.mentionCount DESC")
    List<KgEntity> searchByName(@Param("tenantId") Long tenantId, @Param("kbId") Long kbId,
                                 @Param("q") String q, Pageable pageable);
}

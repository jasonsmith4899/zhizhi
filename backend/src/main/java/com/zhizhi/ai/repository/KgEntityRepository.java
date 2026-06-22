package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.KgEntity;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

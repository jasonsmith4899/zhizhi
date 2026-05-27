package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByKnowledgeBaseIdOrderByCreatedAtDesc(Long knowledgeBaseId);
    long countByKnowledgeBaseId(Long knowledgeBaseId);
    long countByKnowledgeBaseIdAndStatus(Long knowledgeBaseId, String status);

    // 租户隔离查询方法
    List<Document> findByTenantIdAndKnowledgeBaseIdOrderByCreatedAtDesc(Long tenantId, Long knowledgeBaseId);
    long countByTenantIdAndKnowledgeBaseId(Long tenantId, Long knowledgeBaseId);
}

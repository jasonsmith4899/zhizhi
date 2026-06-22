package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByKnowledgeBaseIdOrderByCreatedAtDesc(Long knowledgeBaseId);
    long countByKnowledgeBaseId(Long knowledgeBaseId);
    long countByKnowledgeBaseIdAndStatus(Long knowledgeBaseId, String status);

    // 租户隔离查询方法
    List<Document> findByTenantIdAndKnowledgeBaseIdOrderByCreatedAtDesc(Long tenantId, Long knowledgeBaseId);
    long countByTenantIdAndKnowledgeBaseId(Long tenantId, Long knowledgeBaseId);

    // 秒传去重：同知识库内按内容哈希查找已处理完成的文档
    Optional<Document> findFirstByTenantIdAndKnowledgeBaseIdAndContentHashAndStatus(
            Long tenantId, Long knowledgeBaseId, String contentHash, String status);
}

package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(Long documentId);
    List<DocumentChunk> findByKnowledgeBaseId(Long knowledgeBaseId);
    void deleteByDocumentId(Long documentId);
    long countByDocumentId(Long documentId);
    long countByKnowledgeBaseId(Long knowledgeBaseId);

    // 租户隔离查询方法
    List<DocumentChunk> findByTenantIdAndDocumentIdOrderByChunkIndex(Long tenantId, Long documentId);
    List<DocumentChunk> findByTenantIdAndKnowledgeBaseId(Long tenantId, Long knowledgeBaseId);
    void deleteByTenantIdAndDocumentId(Long tenantId, Long documentId);
    long countByTenantIdAndDocumentId(Long tenantId, Long documentId);
    long countByTenantIdAndKnowledgeBaseId(Long tenantId, Long knowledgeBaseId);
}

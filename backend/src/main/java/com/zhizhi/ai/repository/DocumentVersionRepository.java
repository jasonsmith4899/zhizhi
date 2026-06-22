package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByDocumentIdOrderByVersionNoDesc(Long documentId);
    Optional<DocumentVersion> findByDocumentIdAndVersionNo(Long documentId, Integer versionNo);
    Optional<DocumentVersion> findTopByDocumentIdOrderByVersionNoDesc(Long documentId);
    void deleteByDocumentId(Long documentId);
}

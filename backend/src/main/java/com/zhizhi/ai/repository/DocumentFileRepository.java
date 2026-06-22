package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.DocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentFileRepository extends JpaRepository<DocumentFile, Long> {
    void deleteByDocumentId(Long documentId);
}

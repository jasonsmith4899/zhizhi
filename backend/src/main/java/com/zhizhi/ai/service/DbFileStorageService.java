package com.zhizhi.ai.service;

import com.zhizhi.ai.model.entity.DocumentFile;
import com.zhizhi.ai.repository.DocumentFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基于 PostgreSQL bytea 的原始文件存储实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbFileStorageService implements FileStorageService {

    private final DocumentFileRepository fileRepository;

    @Override
    @Transactional
    public void store(Long documentId, Long tenantId, byte[] data) {
        if (data == null) return;
        DocumentFile file = DocumentFile.builder()
                .documentId(documentId)
                .tenantId(tenantId)
                .data(data)
                .fileSize((long) data.length)
                .build();
        fileRepository.save(file);
    }

    @Override
    public byte[] load(Long documentId) {
        return fileRepository.findById(documentId)
                .map(DocumentFile::getData)
                .orElse(null);
    }

    @Override
    @Transactional
    public void delete(Long documentId) {
        try {
            fileRepository.deleteByDocumentId(documentId);
        } catch (Exception e) {
            log.warn("原始文件删除失败: docId={}, err={}", documentId, e.getMessage());
        }
    }
}

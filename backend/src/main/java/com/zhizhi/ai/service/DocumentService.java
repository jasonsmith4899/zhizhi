package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.entity.Document;
import com.zhizhi.ai.model.entity.DocumentChunk;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import com.zhizhi.ai.repository.DocumentChunkRepository;
import com.zhizhi.ai.repository.DocumentRepository;
import com.zhizhi.ai.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    @Lazy
    private DocumentService self;

    @Value("${app.ai.chunk-size:500}")
    private int chunkSize;

    @Value("${app.ai.chunk-overlap:100}")
    private int chunkOverlap;

    private final Tika tika = new Tika();

    /**
     * 上传文档
     */
    @Transactional
    public Document uploadDocument(Long knowledgeBaseId, MultipartFile file, Long userId) throws IOException {
        // 验证知识库归属
        KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> BusinessException.notFound("知识库"));
        if (!kb.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权操作此知识库");
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw BusinessException.badRequest("缺少租户信息");
        }

        // 验证文件类型
        String filename = file.getOriginalFilename();
        String fileType = getFileType(filename);
        if (fileType == null) {
            throw BusinessException.badRequest("不支持的文件格式，仅支持 PDF/TXT/MD");
        }

        // 创建文档记录
        Document document = Document.builder()
                .knowledgeBase(kb)
                .tenantId(tenantId)
                .filename(filename)
                .fileType(fileType)
                .fileSize(file.getSize())
                .status("processing")
                .build();
        document = documentRepository.save(document);

        // 提取文件字节（避免MultipartFile在异步线程中失效）
        byte[] fileBytes = file.getBytes();

        // 在事务提交后再启动异步处理（避免事务未提交导致读不到document）
        final Long docId = document.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                self.processDocumentAsync(docId, fileBytes, filename, knowledgeBaseId);
            }
        });

        return document;
    }

    /**
     * 异步处理文档：解析 -> 切片 -> 向量化
     */
    @Async
    public void processDocumentAsync(Long documentId, byte[] fileBytes, String filename, Long knowledgeBaseId) {
        try {
            Document document = documentRepository.findById(documentId).orElseThrow();
            Long tenantId = document.getTenantId();

            // 1. 解析文档内容
            String content;
            log.info("fileBytes length: {}", fileBytes.length);
            // 文本文件：先UTF-8解码，检测到乱码则回退GBK
            if (filename != null && (filename.endsWith(".txt") || filename.endsWith(".md") || filename.endsWith(".markdown"))) {
                content = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
                // 如果包含Unicode替换字符（\uFFFD），说明编码不对，尝试GBK
                if (content.contains("\uFFFD")) {
                    log.info("检测到非UTF-8编码，尝试GBK解码: id={}", documentId);
                    content = new String(fileBytes, java.nio.charset.Charset.forName("GBK"));
                }
            } else {
                // PDF等二进制格式用Tika
                try {
                    content = tika.parseToString(new ByteArrayInputStream(fileBytes));
                } catch (org.apache.tika.exception.TikaException e) {
                    throw new IOException("文档解析失败", e);
                }
            }
            document.setContent(content);
            documentRepository.save(document);
            log.info("文档解析完成: id={}, length={}", documentId, content.length());

            // 2. 切片
            List<String> chunks = splitText(content);
            log.info("文档切片完成: id={}, chunks={}", documentId, chunks.size());

            // 3. 向量化并存储到VectorStore
            List<org.springframework.ai.document.Document> aiDocuments = new ArrayList<>();
            List<DocumentChunk> chunkEntities = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                String vectorId = UUID.randomUUID().toString();

                // 创建Spring AI Document（带metadata）
                Map<String, Object> metadata = Map.of(
                        "document_id", documentId.toString(),
                        "knowledge_base_id", knowledgeBaseId.toString(),
                        "tenant_id", tenantId.toString(),
                        "chunk_index", i,
                        "filename", document.getFilename()
                );
                aiDocuments.add(new org.springframework.ai.document.Document(chunk, metadata));

                // 创建切片元数据记录
                chunkEntities.add(DocumentChunk.builder()
                        .documentId(documentId)
                        .knowledgeBaseId(knowledgeBaseId)
                        .tenantId(tenantId)
                        .chunkIndex(i)
                        .content(chunk)
                        .vectorId(vectorId)
                        .build());
            }

            // 批量存储向量
            vectorStore.add(aiDocuments);
            log.info("向量存储完成: id={}, vectors={}", documentId, aiDocuments.size());

            // 保存切片元数据
            chunkRepository.saveAll(chunkEntities);

            // 更新文档状态
            document.setStatus("ready");
            document.setChunkCount(chunks.size());
            documentRepository.save(document);

            // 更新知识库统计
            updateKnowledgeBaseStats(knowledgeBaseId);

            log.info("文档处理完成: id={}, filename={}, chunks={}",
                    documentId, document.getFilename(), chunks.size());

        } catch (Exception e) {
            log.error("文档处理失败: id={}", documentId, e);
            documentRepository.findById(documentId).ifPresent(doc -> {
                doc.setStatus("failed");
                doc.setErrorMessage(e.getMessage());
                documentRepository.save(doc);
            });
        }
    }

    /**
     * 文本切片（简单固定窗口+重叠）
     */
    private List<String> splitText(String content) {
        List<String> chunks = new ArrayList<>();

        // 优先按段落分割
        String[] paragraphs = content.split("\\n\\n+");
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            if (currentChunk.length() + paragraph.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                // 保留overlap
                String existing = currentChunk.toString();
                currentChunk = new StringBuilder(
                        existing.substring(Math.max(0, existing.length() - chunkOverlap)));
            }
            currentChunk.append(paragraph).append("\n\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        // 如果内容太短没有分段，整体作为一个chunk
        if (chunks.isEmpty() && !content.isBlank()) {
            chunks.add(content.trim());
        }

        return chunks;
    }

    /**
     * 根据知识库ID查询文档
     */
    public List<Document> listByKnowledgeBase(Long knowledgeBaseId, Long userId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> BusinessException.notFound("知识库"));
        if (!kb.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权访问此知识库");
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return documentRepository.findByTenantIdAndKnowledgeBaseIdOrderByCreatedAtDesc(tenantId, knowledgeBaseId);
        }
        return documentRepository.findByKnowledgeBaseIdOrderByCreatedAtDesc(knowledgeBaseId);
    }

    /**
     * 删除文档（同时删除向量）
     */
    @Transactional
    public void deleteDocument(Long documentId, Long userId) {
        Document doc = validateDocumentAccess(documentId, userId);
        Long tenantId = TenantContext.getTenantId();

        // 删除VectorStore中的向量（通过metadata过滤）
        // 注意：Spring AI的PgVectorStore可能不支持按metadata过滤删除
        // 这里先删除切片元数据
        if (tenantId != null) {
            chunkRepository.deleteByTenantIdAndDocumentId(tenantId, documentId);
        } else {
            chunkRepository.deleteByDocumentId(documentId);
        }
        documentRepository.delete(doc);

        updateKnowledgeBaseStats(doc.getKnowledgeBase().getId());
    }

    /**
     * 更新知识库统计
     */
    private void updateKnowledgeBaseStats(Long kbId) {
        knowledgeBaseRepository.findById(kbId).ifPresent(kb -> {
            kb.setDocumentCount((int) documentRepository.countByKnowledgeBaseId(kbId));
            kb.setChunkCount((int) chunkRepository.countByKnowledgeBaseId(kbId));
            knowledgeBaseRepository.save(kb);
        });
    }

    public List<DocumentChunk> getDocumentChunks(Long documentId, Long userId) {
        validateDocumentAccess(documentId, userId);
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return chunkRepository.findByTenantIdAndDocumentIdOrderByChunkIndex(tenantId, documentId);
        }
        return chunkRepository.findByDocumentIdOrderByChunkIndex(documentId);
    }

    public Map<String, Object> getVectorStatus(Long documentId, Long userId) {
        Document doc = validateDocumentAccess(documentId, userId);
        Long tenantId = TenantContext.getTenantId();
        long chunkCount;
        if (tenantId != null) {
            chunkCount = chunkRepository.countByTenantIdAndDocumentId(tenantId, documentId);
        } else {
            chunkCount = chunkRepository.countByDocumentId(documentId);
        }

        Integer vectorCount = 0;
        try {
            vectorCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_chunks WHERE metadata->>'document_id' = ?",
                Integer.class, documentId.toString());
        } catch (Exception e) {
            log.warn("Failed to query vector count", e);
        }

        boolean needsReVectorization = "failed".equals(doc.getStatus()) ||
            (doc.getChunkCount() == null || doc.getChunkCount() == 0) ||
            (chunkCount > 0 && (vectorCount == null || vectorCount == 0));

        Map<String, Object> result = new HashMap<>();
        result.put("documentId", documentId);
        result.put("status", doc.getStatus());
        result.put("chunkCount", chunkCount);
        result.put("vectorCount", vectorCount != null ? vectorCount : 0);
        result.put("errorMessage", doc.getErrorMessage());
        result.put("needsReVectorization", needsReVectorization);
        return result;
    }

    @Transactional
    public void reVectorize(Long documentId, Long userId) {
        Document doc = validateDocumentAccess(documentId, userId);
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            chunkRepository.deleteByTenantIdAndDocumentId(tenantId, documentId);
        } else {
            chunkRepository.deleteByDocumentId(documentId);
        }

        try {
            jdbcTemplate.update("DELETE FROM vector_chunks WHERE metadata->>'document_id' = ?",
                documentId.toString());
        } catch (Exception e) {
            log.warn("Failed to delete vectors from vector store", e);
        }

        doc.setStatus("processing");
        doc.setChunkCount(0);
        doc.setErrorMessage(null);
        documentRepository.save(doc);

        Long kbId = doc.getKnowledgeBase().getId();
        String filename = doc.getFilename();
        String content = doc.getContent();
        if (content != null && !content.isEmpty()) {
            byte[] contentBytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            self.processDocumentAsync(documentId, contentBytes, filename, kbId);
        } else {
            log.warn("Document content is empty, cannot re-vectorize: id={}", documentId);
            doc.setStatus("failed");
            doc.setErrorMessage("文档内容为空，无法重新向量化");
            documentRepository.save(doc);
        }
    }

    /**
     * 获取文档预览内容
     */
    public Map<String, Object> getDocumentPreview(Long documentId, Long userId) {
        Document doc = validateDocumentAccess(documentId, userId);
        Long tenantId = TenantContext.getTenantId();
        List<DocumentChunk> chunks;
        if (tenantId != null) {
            chunks = chunkRepository.findByTenantIdAndDocumentIdOrderByChunkIndex(tenantId, documentId);
        } else {
            chunks = chunkRepository.findByDocumentIdOrderByChunkIndex(documentId);
        }

        StringBuilder content = new StringBuilder();
        for (DocumentChunk chunk : chunks) {
            if (chunk.getContent() != null) {
                content.append(chunk.getContent()).append("\n\n");
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("documentId", documentId);
        result.put("documentName", doc.getFilename());
        result.put("contentType", doc.getFileType());
        result.put("content", content.toString().trim());
        result.put("chunkCount", chunks.size());
        return result;
    }

    /**
     * 获取文档下载内容
     */
    public Map<String, Object> getDocumentDownload(Long documentId, Long userId) {
        Document doc = validateDocumentAccess(documentId, userId);
        Long tenantId = TenantContext.getTenantId();
        List<DocumentChunk> chunks;
        if (tenantId != null) {
            chunks = chunkRepository.findByTenantIdAndDocumentIdOrderByChunkIndex(tenantId, documentId);
        } else {
            chunks = chunkRepository.findByDocumentIdOrderByChunkIndex(documentId);
        }

        StringBuilder content = new StringBuilder();
        for (DocumentChunk chunk : chunks) {
            if (chunk.getContent() != null) {
                content.append(chunk.getContent()).append("\n\n");
            }
        }

        String filename = doc.getFilename();
        String fileType = doc.getFileType();
        String downloadFilename;
        String contentType;

        if ("pdf".equals(fileType)) {
            downloadFilename = filename.replaceAll("(?i)\\.pdf$", "") + ".txt";
            contentType = "text/plain; charset=utf-8";
        } else if ("md".equals(fileType) || "markdown".equals(fileType)) {
            downloadFilename = filename;
            contentType = "text/markdown; charset=utf-8";
        } else {
            downloadFilename = filename;
            contentType = "text/plain; charset=utf-8";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("filename", downloadFilename);
        result.put("content", content.toString().trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        result.put("contentType", contentType);
        return result;
    }

    @Transactional
    public void batchDelete(List<Long> documentIds, Long userId) {
        if (documentIds == null || documentIds.isEmpty()) {
            throw BusinessException.badRequest("请选择要删除的文档");
        }

        Long tenantId = TenantContext.getTenantId();
        Long kbId = null;
        for (Long docId : documentIds) {
            Document doc = documentRepository.findById(docId)
                    .orElseThrow(() -> BusinessException.notFound("文档"));
            KnowledgeBase kb = knowledgeBaseRepository.findById(doc.getKnowledgeBase().getId()).orElseThrow();
            if (!kb.getUserId().equals(userId)) {
                throw BusinessException.forbidden("无权删除文档: " + doc.getFilename());
            }
            if (tenantId != null && !doc.getTenantId().equals(tenantId)) {
                throw BusinessException.forbidden("无权删除文档: " + doc.getFilename());
            }
            if (kbId == null) {
                kbId = kb.getId();
            }

            if (tenantId != null) {
                chunkRepository.deleteByTenantIdAndDocumentId(tenantId, docId);
            } else {
                chunkRepository.deleteByDocumentId(docId);
            }
            try {
                jdbcTemplate.update("DELETE FROM vector_chunks WHERE metadata->>'document_id' = ?",
                    docId.toString());
            } catch (Exception e) {
                log.warn("Failed to delete vectors for document {}", docId, e);
            }
            documentRepository.delete(doc);
        }

        if (kbId != null) {
            updateKnowledgeBaseStats(kbId);
        }
    }

    /**
     * 校验文档访问权限：查文档→查知识库→校验userId→校验tenantId
     */
    private Document validateDocumentAccess(Long documentId, Long userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> BusinessException.notFound("文档"));
        KnowledgeBase kb = knowledgeBaseRepository.findById(doc.getKnowledgeBase().getId())
                .orElseThrow(() -> BusinessException.notFound("知识库"));
        if (!kb.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权访问此文档");
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !doc.getTenantId().equals(tenantId)) {
            throw BusinessException.forbidden("无权访问此文档");
        }
        return doc;
    }

    /**
     * 获取文件类型
     */
    private String getFileType(String filename) {
        if (filename == null) return null;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".txt")) return "txt";
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) return "md";
        return null;
    }
}

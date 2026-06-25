package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.entity.Document;
import com.zhizhi.ai.model.entity.DocumentChunk;
import com.zhizhi.ai.model.entity.DocumentVersion;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import com.zhizhi.ai.repository.DocumentChunkRepository;
import com.zhizhi.ai.repository.DocumentRepository;
import com.zhizhi.ai.repository.DocumentTagRepository;
import com.zhizhi.ai.repository.DocumentVersionRepository;
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
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final KnowledgeGraphService knowledgeGraphService;
    private final FileStorageService fileStorageService;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentTagRepository documentTagRepository;
    private final TagService tagService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

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
        if (!Objects.equals(kb.getUserId(), userId)) {
            throw BusinessException.forbidden("无权操作此知识库");
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw BusinessException.badRequest("缺少租户信息");
        }

        // 验证文件不为空
        if (file.isEmpty() || file.getSize() == 0) {
            throw BusinessException.badRequest("上传文件不能为空");
        }

        // 验证文件类型
        String filename = file.getOriginalFilename();
        String fileType = getFileType(filename);
        if (fileType == null) {
            throw BusinessException.badRequest("不支持的文件格式，仅支持 PDF/TXT/MD");
        }

        // 提取文件字节（避免MultipartFile在异步线程中失效）
        byte[] fileBytes = file.getBytes();
        String hash = sha256(fileBytes);

        // 秒传：同知识库内已存在相同内容且处理完成的文档，直接复用，跳过存储与向量化
        Document existing = documentRepository
                .findFirstByTenantIdAndKnowledgeBaseIdAndContentHashAndStatus(tenantId, knowledgeBaseId, hash, "ready")
                .orElse(null);
        if (existing != null) {
            log.info("秒传命中，跳过重复处理: hash={}, 复用 docId={}", hash, existing.getId());
            return existing;
        }

        // 创建文档记录
        Document document = Document.builder()
                .knowledgeBase(kb)
                .tenantId(tenantId)
                .filename(filename)
                .fileType(fileType)
                .fileSize(file.getSize())
                .contentHash(hash)
                .mimeType(file.getContentType())
                .status("processing")
                .build();
        document = documentRepository.save(document);

        // 存储原始文件（供在线预览）
        fileStorageService.store(document.getId(), tenantId, fileBytes);

        // 发布处理事件，由监听器在事务提交后异步处理（解析/切片/向量化/图谱解耦）
        eventPublisher.publishEvent(new com.zhizhi.ai.event.DocumentProcessEvent(
                document.getId(), fileBytes, filename, knowledgeBaseId));

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

            // 2. 切片（递归策略 + 标题边界 + 标题层级元数据）
            List<ChunkWithMeta> chunkResults = splitText(content);
            log.info("文档切片完成: id={}, chunks={}", documentId, chunkResults.size());

            // 3. 向量化并存储到VectorStore
            List<org.springframework.ai.document.Document> aiDocuments = new ArrayList<>();
            List<DocumentChunk> chunkEntities = new ArrayList<>();

            for (int i = 0; i < chunkResults.size(); i++) {
                ChunkWithMeta cm = chunkResults.get(i);
                String vectorId = UUID.randomUUID().toString();

                // 创建Spring AI Document（带metadata）
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("document_id", documentId.toString());
                metadata.put("knowledge_base_id", knowledgeBaseId.toString());
                metadata.put("tenant_id", tenantId.toString());
                metadata.put("chunk_index", i);
                metadata.put("filename", document.getFilename());
                if (cm.headingHierarchy() != null && !cm.headingHierarchy().isEmpty()) {
                    metadata.put("heading_hierarchy", cm.headingHierarchy());
                }
                aiDocuments.add(new org.springframework.ai.document.Document(cm.text(), metadata));

                // 创建切片元数据记录
                chunkEntities.add(DocumentChunk.builder()
                        .documentId(documentId)
                        .knowledgeBaseId(knowledgeBaseId)
                        .tenantId(tenantId)
                        .chunkIndex(i)
                        .content(cm.text())
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
            document.setChunkCount(chunkResults.size());
            documentRepository.save(document);

            // 更新知识库统计
            updateKnowledgeBaseStats(knowledgeBaseId);

            log.info("文档处理完成: id={}, filename={}, chunks={}",
                    documentId, document.getFilename(), chunkResults.size());

            // KAG：抽取实体关系构建知识图谱（增强项，失败不影响主流程）
            knowledgeGraphService.extractAndStore(documentId, knowledgeBaseId, tenantId, content);

            // 版本管理：每次成功处理后保存内容快照
            saveVersionSnapshot(documentId, tenantId, content, chunkResults.size());

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
     * 切片结果：文本 + 标题层级链
     */
    private record ChunkWithMeta(String text, String headingHierarchy) {}

    /** Markdown 标题正则 */
    private static final Pattern HEADING_PATTERN =
            Pattern.compile("^(#{1,4})\\s+(.+)$", Pattern.MULTILINE);

    /**
     * 文本切片（递归策略 + 标题边界强制 + 标题层级元数据）
     *
     * 策略：
     * 1. 按 Markdown 标题强制分界，标题必须是 chunk 开头
     * 2. 每个 section 的正文递归切分：段落 → 句子 → 字符兜底
     * 3. 保留 chunkOverlap 重叠（不跨标题边界）
     * 4. 每个 chunk 记录父级标题链（heading hierarchy）
     */
    private List<ChunkWithMeta> splitText(String content) {
        List<ChunkWithMeta> result = new ArrayList<>();
        if (content == null || content.isBlank()) return result;

        // 按 Markdown 标题分割
        Matcher matcher = HEADING_PATTERN.matcher(content);
        List<int[]> headingPositions = new ArrayList<>();
        while (matcher.find()) {
            headingPositions.add(new int[]{matcher.start(), matcher.end()});
        }

        // 无标题时，整体递归切分
        if (headingPositions.isEmpty()) {
            for (String chunk : recursiveSplit(content)) {
                if (!chunk.isBlank()) result.add(new ChunkWithMeta(chunk.trim(), ""));
            }
            return result;
        }

        // 维护标题层级
        String[] levelHeadings = new String[5]; // index 1-4 对应 # ~ ####

        // 处理第一个标题前的内容（无标题层级）
        int firstStart = headingPositions.get(0)[0];
        if (firstStart > 0) {
            String intro = content.substring(0, firstStart).trim();
            if (!intro.isEmpty()) {
                for (String chunk : recursiveSplit(intro)) {
                    if (!chunk.isBlank()) result.add(new ChunkWithMeta(chunk.trim(), ""));
                }
            }
        }

        // 处理每个标题区块
        for (int i = 0; i < headingPositions.size(); i++) {
            int secStart = headingPositions.get(i)[0];
            int secEnd = (i + 1 < headingPositions.size())
                    ? headingPositions.get(i + 1)[0] : content.length();
            String section = content.substring(secStart, secEnd).trim();

            // 解析标题行和层级
            String headingLine = section.split("\n", 2)[0].trim();
            Matcher lvlMatcher = Pattern.compile("^(#{1,4})\\s+").matcher(headingLine);
            int level = 1;
            if (lvlMatcher.find()) {
                level = lvlMatcher.group(1).length();
            }

            // 更新标题层级（清除更深层级）
            levelHeadings[level] = headingLine;
            for (int j = level + 1; j <= 4; j++) levelHeadings[j] = null;
            String hierarchy = buildHierarchy(levelHeadings, level);

            // 提取正文（标题行之后的内容）
            String body = "";
            int nl = section.indexOf('\n');
            if (nl >= 0) body = section.substring(nl + 1).trim();

            if (body.isEmpty()) continue;

            // 递归切分正文
            List<String> bodyChunks = recursiveSplit(body);
            for (int ci = 0; ci < bodyChunks.size(); ci++) {
                String chunkText = bodyChunks.get(ci).trim();
                if (chunkText.isEmpty()) continue;
                // 第一个 chunk 以标题开头（标题作为 chunk 边界）
                if (ci == 0) {
                    chunkText = headingLine + "\n" + chunkText;
                }
                result.add(new ChunkWithMeta(chunkText, hierarchy));
            }
        }

        if (result.isEmpty() && !content.isBlank()) {
            result.add(new ChunkWithMeta(content.trim(), ""));
        }
        return result;
    }

    /**
     * 构建标题层级链，如 "## AI集成架构 > ### ChatClient"
     */
    private String buildHierarchy(String[] levelHeadings, int maxLevel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= maxLevel; i++) {
            if (levelHeadings[i] != null) {
                if (sb.length() > 0) sb.append(" > ");
                sb.append(levelHeadings[i]);
            }
        }
        return sb.toString();
    }

    /**
     * 递归切分正文：段落 → 句子 → 字符兜底
     */
    private List<String> recursiveSplit(String text) {
        if (text.length() <= chunkSize) {
            return new ArrayList<>(List.of(text));
        }
        return splitByLevel(text, 0);
    }

    /**
     * 按层级递归切分
     * @param level 0=段落(\n\n), 1=句子(。！？.!?), 2=字符硬切
     */
    private List<String> splitByLevel(String text, int level) {
        if (text.length() <= chunkSize) {
            return new ArrayList<>(List.of(text));
        }
        if (level >= 2) {
            return hardSplit(text);
        }

        // 按当前层级的分隔符切分
        List<String> pieces;
        if (level == 0) {
            String[] parts = text.split("\\n\\n+", -1);
            pieces = new ArrayList<>();
            for (String p : parts) {
                if (!p.isBlank()) pieces.add(p);
            }
        } else {
            pieces = splitBySentences(text);
        }

        if (pieces.isEmpty()) {
            return hardSplit(text);
        }

        // 合并小片段，递归切分大片段
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String piece : pieces) {
            if (piece.length() > chunkSize) {
                // 先保存已累积的 current
                if (current.length() > 0) {
                    chunks.add(current.toString().trim());
                    current = new StringBuilder(overlapTail(current.toString()));
                }
                // 递归切分超大片段
                List<String> subChunks = splitByLevel(piece, level + 1);
                for (int si = 0; si < subChunks.size() - 1; si++) {
                    chunks.add(subChunks.get(si));
                }
                if (!subChunks.isEmpty()) {
                    if (current.length() > 0 && level == 0) current.append("\n\n");
                    current.append(subChunks.get(subChunks.size() - 1));
                }
            } else {
                String sep = level == 0 ? "\n\n" : "";
                String combined = current.length() > 0
                        ? current.toString() + sep + piece : piece;
                if (combined.length() > chunkSize && current.length() > 0) {
                    chunks.add(current.toString().trim());
                    current = new StringBuilder(overlapTail(current.toString()));
                    current.append(piece);
                } else {
                    current = new StringBuilder(combined);
                }
            }
        }

        if (current.length() > 0 && !current.toString().isBlank()) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    /**
     * 按句子切分（在句号后分割）
     */
    private List<String> splitBySentences(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            current.append(text.charAt(i));
            if ("。！？.!?".indexOf(text.charAt(i)) >= 0) {
                sentences.add(current.toString());
                current = new StringBuilder();
            }
        }
        if (current.length() > 0) sentences.add(current.toString());
        return sentences;
    }

    /**
     * 获取文本末尾 overlap 长度的内容（用于重叠）
     */
    private String overlapTail(String text) {
        if (text.length() <= chunkOverlap) return text;
        return text.substring(text.length() - chunkOverlap);
    }

    /**
     * 硬切：按 chunkSize 固定窗口切分，保留重叠
     */
    private List<String> hardSplit(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) chunks.add(chunk);
            start += chunkSize - chunkOverlap;
            if (start >= text.length()) break;
        }
        return chunks;
    }

    /**
     * 根据知识库ID查询文档
     */
    public List<Document> listByKnowledgeBase(Long knowledgeBaseId, Long userId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> BusinessException.notFound("知识库"));
        if (!Objects.equals(kb.getUserId(), userId)) {
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
        // 清理版本与标签关联
        documentVersionRepository.deleteByDocumentId(documentId);
        documentTagRepository.deleteByDocumentId(documentId);

        documentRepository.delete(doc);

        // 清理 KAG 图谱关系
        knowledgeGraphService.deleteByDocument(documentId);

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

        // 清理旧的 KAG 图谱关系（重新向量化时会重新抽取）
        knowledgeGraphService.deleteByDocument(documentId);

        doc.setStatus("processing");
        doc.setChunkCount(0);
        doc.setErrorMessage(null);
        documentRepository.save(doc);

        Long kbId = doc.getKnowledgeBase().getId();
        String filename = doc.getFilename();
        String content = doc.getContent();
        if (content != null && !content.isEmpty()) {
            byte[] contentBytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            eventPublisher.publishEvent(new com.zhizhi.ai.event.DocumentProcessEvent(
                    documentId, contentBytes, filename, kbId));
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

    // ==================== 分类 / 标签 / 版本 ====================

    /** 设置文档归属分类（categoryId 为 null 表示取消归类） */
    @Transactional
    public Document setCategory(Long documentId, Long categoryId, Long userId) {
        Document doc = validateDocumentAccess(documentId, userId);
        doc.setCategoryId(categoryId);
        return documentRepository.save(doc);
    }

    /** 设置文档标签（全量覆盖） */
    @Transactional
    public void setTags(Long documentId, List<Long> tagIds, Long userId) {
        validateDocumentAccess(documentId, userId);
        tagService.setDocumentTags(documentId, tagIds, TenantContext.getTenantId());
    }

    /** 获取文档标签 ID 列表 */
    public List<Long> getDocumentTagIds(Long documentId, Long userId) {
        validateDocumentAccess(documentId, userId);
        return tagService.getDocumentTagIds(documentId);
    }

    /** 版本列表（按版本号倒序） */
    public List<DocumentVersion> listVersions(Long documentId, Long userId) {
        validateDocumentAccess(documentId, userId);
        return documentVersionRepository.findByDocumentIdOrderByVersionNoDesc(documentId);
    }

    /** 回滚到指定版本：用历史内容重建切片与向量 */
    @Transactional
    public void rollback(Long documentId, Integer versionNo, Long userId) {
        Document doc = validateDocumentAccess(documentId, userId);
        DocumentVersion v = documentVersionRepository.findByDocumentIdAndVersionNo(documentId, versionNo)
                .orElseThrow(() -> BusinessException.notFound("文档版本"));
        if (v.getContent() == null || v.getContent().isEmpty()) {
            throw BusinessException.badRequest("该版本无内容，无法回滚");
        }
        doc.setContent(v.getContent());
        documentRepository.save(doc);
        // 复用重新向量化链路：用回滚后的 content 重建（完成后会自动生成新版本快照）
        reVectorize(documentId, userId);
    }

    /** 保存内容快照为新版本（在异步处理线程中调用） */
    private void saveVersionSnapshot(Long documentId, Long tenantId, String content, int chunkCount) {
        try {
            int nextNo = documentVersionRepository.findTopByDocumentIdOrderByVersionNoDesc(documentId)
                    .map(v -> v.getVersionNo() + 1).orElse(1);
            documentVersionRepository.save(DocumentVersion.builder()
                    .documentId(documentId)
                    .tenantId(tenantId)
                    .versionNo(nextNo)
                    .content(content)
                    .chunkCount(chunkCount)
                    .remark(nextNo == 1 ? "首次上传" : "重新处理快照")
                    .build());
        } catch (Exception e) {
            log.warn("保存文档版本快照失败: docId={}, err={}", documentId, e.getMessage());
        }
    }

    /**
     * 获取原始文件字节（供前端原样预览：PDF 渲染、文本/Markdown 等）
     */
    public Map<String, Object> getRawFile(Long documentId, Long userId) {
        Document doc = validateDocumentAccess(documentId, userId);
        byte[] data = fileStorageService.load(documentId);
        if (data == null) {
            throw BusinessException.notFound("原始文件（该文档上传于旧版本，无原始文件）");
        }
        String mime = doc.getMimeType();
        if (mime == null || mime.isBlank()) {
            mime = guessMime(doc.getFileType());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("filename", doc.getFilename());
        result.put("content", data);
        result.put("contentType", mime);
        return result;
    }

    private String guessMime(String fileType) {
        if (fileType == null) return "application/octet-stream";
        return switch (fileType) {
            case "pdf" -> "application/pdf";
            case "md", "markdown" -> "text/markdown; charset=utf-8";
            case "txt" -> "text/plain; charset=utf-8";
            default -> "application/octet-stream";
        };
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
            if (!Objects.equals(kb.getUserId(), userId)) {
                throw BusinessException.forbidden("无权删除文档: " + doc.getFilename());
            }
            if (tenantId != null && !Objects.equals(doc.getTenantId(), tenantId)) {
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
            documentVersionRepository.deleteByDocumentId(docId);
            documentTagRepository.deleteByDocumentId(docId);
            knowledgeGraphService.deleteByDocument(docId);
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
        if (!Objects.equals(kb.getUserId(), userId)) {
            throw BusinessException.forbidden("无权访问此文档");
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !Objects.equals(doc.getTenantId(), tenantId)) {
            throw BusinessException.forbidden("无权访问此文档");
        }
        return doc;
    }

    /**
     * 计算文件 SHA-256（十六进制）
     */
    private String sha256(byte[] bytes) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return null;
        }
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

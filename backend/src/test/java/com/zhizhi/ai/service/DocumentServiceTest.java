package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.event.DocumentProcessEvent;
import com.zhizhi.ai.model.entity.*;
import com.zhizhi.ai.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService 单元测试")
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock private VectorStore vectorStore;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private KnowledgeGraphService knowledgeGraphService;
    @Mock private FileStorageService fileStorageService;
    @Mock private DocumentVersionRepository documentVersionRepository;
    @Mock private DocumentTagRepository documentTagRepository;
    @Mock private TagService tagService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DocumentService documentService;

    private static final Long TENANT_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final Long KB_ID = 10L;
    private static final Long DOC_ID = 50L;

    private KnowledgeBase sampleKb;
    private Document sampleDoc;

    @BeforeEach
    void setUp() {
        sampleKb = KnowledgeBase.builder()
                .id(KB_ID).userId(USER_ID).tenantId(TENANT_ID).name("KB").build();
        sampleDoc = Document.builder()
                .id(DOC_ID)
                .knowledgeBase(sampleKb)
                .tenantId(TENANT_ID)
                .filename("test.pdf")
                .fileType("pdf")
                .fileSize(1024L)
                .status("ready")
                .chunkCount(5)
                .build();
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    // ==================== uploadDocument ====================

    @Nested
    @DisplayName("uploadDocument")
    class UploadDocumentTests {

        @Test
        @DisplayName("上传PDF文件成功")
        void uploadDocument_success() throws IOException {
            TenantContext.setTenantId(TENANT_ID);
            byte[] fileContent = "PDF content bytes".getBytes();
            MultipartFile file = new MockMultipartFile("file", "test.pdf",
                    "application/pdf", fileContent);

            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            when(documentRepository.findFirstByTenantIdAndKnowledgeBaseIdAndContentHashAndStatus(
                    eq(TENANT_ID), eq(KB_ID), anyString(), eq("ready")))
                    .thenReturn(Optional.empty());
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document d = inv.getArgument(0);
                d.setId(DOC_ID);
                return d;
            });

            Document result = documentService.uploadDocument(KB_ID, file, USER_ID);

            assertThat(result.getId()).isEqualTo(DOC_ID);
            assertThat(result.getStatus()).isEqualTo("processing");
            assertThat(result.getFilename()).isEqualTo("test.pdf");
            assertThat(result.getFileType()).isEqualTo("pdf");
            verify(fileStorageService).store(eq(DOC_ID), eq(TENANT_ID), eq(fileContent));
            verify(eventPublisher).publishEvent(any(DocumentProcessEvent.class));
        }

        @Test
        @DisplayName("上传TXT文件成功")
        void uploadDocument_txtFile() throws IOException {
            TenantContext.setTenantId(TENANT_ID);
            MultipartFile file = new MockMultipartFile("file", "readme.txt",
                    "text/plain", "Hello world".getBytes());

            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            when(documentRepository.findFirstByTenantIdAndKnowledgeBaseIdAndContentHashAndStatus(
                    any(), any(), any(), any())).thenReturn(Optional.empty());
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document d = inv.getArgument(0);
                d.setId(DOC_ID);
                return d;
            });

            Document result = documentService.uploadDocument(KB_ID, file, USER_ID);

            assertThat(result.getFileType()).isEqualTo("txt");
        }

        @Test
        @DisplayName("上传MD文件成功")
        void uploadDocument_mdFile() throws IOException {
            TenantContext.setTenantId(TENANT_ID);
            MultipartFile file = new MockMultipartFile("file", "doc.md",
                    "text/markdown", "# Title".getBytes());

            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            when(documentRepository.findFirstByTenantIdAndKnowledgeBaseIdAndContentHashAndStatus(
                    any(), any(), any(), any())).thenReturn(Optional.empty());
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document d = inv.getArgument(0);
                d.setId(DOC_ID);
                return d;
            });

            Document result = documentService.uploadDocument(KB_ID, file, USER_ID);

            assertThat(result.getFileType()).isEqualTo("md");
        }

        @Test
        @DisplayName("秒传命中 - 跳过重复处理")
        void uploadDocument_instantUpload() throws IOException {
            TenantContext.setTenantId(TENANT_ID);
            MultipartFile file = new MockMultipartFile("file", "test.pdf",
                    "application/pdf", "content".getBytes());

            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            when(documentRepository.findFirstByTenantIdAndKnowledgeBaseIdAndContentHashAndStatus(
                    eq(TENANT_ID), eq(KB_ID), anyString(), eq("ready")))
                    .thenReturn(Optional.of(sampleDoc));

            Document result = documentService.uploadDocument(KB_ID, file, USER_ID);

            assertThat(result).isEqualTo(sampleDoc);
            verify(documentRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("上传失败 - 知识库不存在")
        void uploadDocument_kbNotFound() {
            when(knowledgeBaseRepository.findById(999L)).thenReturn(Optional.empty());
            MultipartFile file = new MockMultipartFile("file", "test.pdf",
                    "application/pdf", "data".getBytes());

            assertThatThrownBy(() -> documentService.uploadDocument(999L, file, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(404));
        }

        @Test
        @DisplayName("上传失败 - 无权操作他人知识库")
        void uploadDocument_forbidden() {
            KnowledgeBase otherKb = KnowledgeBase.builder().id(KB_ID).userId(2L).build();
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(otherKb));
            MultipartFile file = new MockMultipartFile("file", "test.pdf",
                    "application/pdf", "data".getBytes());

            assertThatThrownBy(() -> documentService.uploadDocument(KB_ID, file, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(403));
        }

        @Test
        @DisplayName("上传失败 - 缺少租户信息")
        void uploadDocument_noTenant() {
            TenantContext.clear();
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            MultipartFile file = new MockMultipartFile("file", "test.pdf",
                    "application/pdf", "data".getBytes());

            assertThatThrownBy(() -> documentService.uploadDocument(KB_ID, file, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("租户");
        }

        @Test
        @DisplayName("上传失败 - 空文件")
        void uploadDocument_emptyFile() throws IOException {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(true);

            assertThatThrownBy(() -> documentService.uploadDocument(KB_ID, file, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能为空");
        }

        @Test
        @DisplayName("上传失败 - 不支持的文件格式")
        void uploadDocument_unsupportedFormat() throws IOException {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            MultipartFile file = new MockMultipartFile("file", "image.png",
                    "image/png", "data".getBytes());

            assertThatThrownBy(() -> documentService.uploadDocument(KB_ID, file, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不支持的文件格式");
        }
    }

    // ==================== listByKnowledgeBase ====================

    @Nested
    @DisplayName("listByKnowledgeBase")
    class ListByKnowledgeBaseTests {

        @Test
        @DisplayName("按租户查询文档列表")
        void listByKnowledgeBase_withTenant() {
            TenantContext.setTenantId(TENANT_ID);
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            when(documentRepository.findByTenantIdAndKnowledgeBaseIdOrderByCreatedAtDesc(TENANT_ID, KB_ID))
                    .thenReturn(List.of(sampleDoc));

            List<Document> result = documentService.listByKnowledgeBase(KB_ID, USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(DOC_ID);
        }

        @Test
        @DisplayName("无租户时按知识库查询")
        void listByKnowledgeBase_noTenant() {
            TenantContext.clear();
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            when(documentRepository.findByKnowledgeBaseIdOrderByCreatedAtDesc(KB_ID))
                    .thenReturn(List.of(sampleDoc));

            List<Document> result = documentService.listByKnowledgeBase(KB_ID, USER_ID);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("查询失败 - 知识库不存在")
        void listByKnowledgeBase_kbNotFound() {
            when(knowledgeBaseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.listByKnowledgeBase(999L, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(404));
        }

        @Test
        @DisplayName("查询失败 - 无权访问")
        void listByKnowledgeBase_forbidden() {
            KnowledgeBase otherKb = KnowledgeBase.builder().id(KB_ID).userId(2L).build();
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(otherKb));

            assertThatThrownBy(() -> documentService.listByKnowledgeBase(KB_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(403));
        }
    }

    // ==================== deleteDocument ====================

    @Nested
    @DisplayName("deleteDocument")
    class DeleteDocumentTests {

        @Test
        @DisplayName("删除成功")
        void deleteDocument_success() {
            TenantContext.setTenantId(TENANT_ID);
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(sampleDoc));
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));

            documentService.deleteDocument(DOC_ID, USER_ID);

            verify(chunkRepository).deleteByTenantIdAndDocumentId(TENANT_ID, DOC_ID);
            verify(documentVersionRepository).deleteByDocumentId(DOC_ID);
            verify(documentTagRepository).deleteByDocumentId(DOC_ID);
            verify(documentRepository).delete(sampleDoc);
            verify(knowledgeGraphService).deleteByDocument(DOC_ID);
        }

        @Test
        @DisplayName("删除成功 - 无租户时使用非租户删除方法")
        void deleteDocument_noTenant() {
            TenantContext.clear();
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(sampleDoc));
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));

            documentService.deleteDocument(DOC_ID, USER_ID);

            verify(chunkRepository).deleteByDocumentId(DOC_ID);
            verify(documentRepository).delete(sampleDoc);
        }

        @Test
        @DisplayName("删除失败 - 文档不存在")
        void deleteDocument_notFound() {
            when(documentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.deleteDocument(999L, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(404));
        }

        @Test
        @DisplayName("删除失败 - 无权访问")
        void deleteDocument_forbidden() {
            TenantContext.setTenantId(TENANT_ID);
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(sampleDoc));
            KnowledgeBase otherKb = KnowledgeBase.builder().id(KB_ID).userId(2L).build();
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(otherKb));

            assertThatThrownBy(() -> documentService.deleteDocument(DOC_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(403));
        }
    }

    // ==================== reVectorize ====================

    @Nested
    @DisplayName("reVectorize")
    class ReVectorizeTests {

        @Test
        @DisplayName("重新向量化成功 - 有内容")
        void reVectorize_success() {
            TenantContext.setTenantId(TENANT_ID);
            sampleDoc.setContent("Some document content");
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(sampleDoc));
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            documentService.reVectorize(DOC_ID, USER_ID);

            verify(chunkRepository).deleteByTenantIdAndDocumentId(TENANT_ID, DOC_ID);
            verify(jdbcTemplate).update(contains("DELETE FROM vector_chunks"), eq(DOC_ID.toString()));
            verify(knowledgeGraphService).deleteByDocument(DOC_ID);
            assertThat(sampleDoc.getStatus()).isEqualTo("processing");
            assertThat(sampleDoc.getChunkCount()).isEqualTo(0);
            verify(eventPublisher).publishEvent(any(DocumentProcessEvent.class));
        }

        @Test
        @DisplayName("重新向量化失败 - 文档内容为空")
        void reVectorize_emptyContent() {
            TenantContext.setTenantId(TENANT_ID);
            sampleDoc.setContent(null);
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(sampleDoc));
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            documentService.reVectorize(DOC_ID, USER_ID);

            assertThat(sampleDoc.getStatus()).isEqualTo("failed");
            assertThat(sampleDoc.getErrorMessage()).contains("内容为空");
        }

        @Test
        @DisplayName("重新向量化失败 - 文档不存在")
        void reVectorize_notFound() {
            when(documentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.reVectorize(999L, USER_ID))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ==================== rollback ====================

    @Nested
    @DisplayName("rollback")
    class RollbackTests {

        @Test
        @DisplayName("回滚成功")
        void rollback_success() {
            TenantContext.setTenantId(TENANT_ID);
            sampleDoc.setContent("Current content");
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(sampleDoc));
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            DocumentVersion version = DocumentVersion.builder()
                    .id(1L).documentId(DOC_ID).tenantId(TENANT_ID)
                    .versionNo(1).content("Old content").chunkCount(3).build();
            when(documentVersionRepository.findByDocumentIdAndVersionNo(DOC_ID, 1))
                    .thenReturn(Optional.of(version));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            documentService.rollback(DOC_ID, 1, USER_ID);

            assertThat(sampleDoc.getContent()).isEqualTo("Old content");
            verify(documentRepository, atLeastOnce()).save(sampleDoc);
        }

        @Test
        @DisplayName("回滚失败 - 版本不存在")
        void rollback_versionNotFound() {
            TenantContext.setTenantId(TENANT_ID);
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(sampleDoc));
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            when(documentVersionRepository.findByDocumentIdAndVersionNo(DOC_ID, 99))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.rollback(DOC_ID, 99, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(404));
        }

        @Test
        @DisplayName("回滚失败 - 版本内容为空")
        void rollback_emptyVersionContent() {
            TenantContext.setTenantId(TENANT_ID);
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(sampleDoc));
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            DocumentVersion emptyVersion = DocumentVersion.builder()
                    .id(1L).documentId(DOC_ID).versionNo(1).content("").build();
            when(documentVersionRepository.findByDocumentIdAndVersionNo(DOC_ID, 1))
                    .thenReturn(Optional.of(emptyVersion));

            assertThatThrownBy(() -> documentService.rollback(DOC_ID, 1, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无内容");
        }
    }

    // ==================== setCategory ====================

    @Nested
    @DisplayName("setCategory")
    class SetCategoryTests {

        @Test
        @DisplayName("设置分类成功")
        void setCategory_success() {
            TenantContext.setTenantId(TENANT_ID);
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(sampleDoc));
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            Document result = documentService.setCategory(DOC_ID, 5L, USER_ID);

            assertThat(result.getCategoryId()).isEqualTo(5L);
            verify(documentRepository).save(sampleDoc);
        }

        @Test
        @DisplayName("取消分类 - categoryId为null")
        void setCategory_null() {
            TenantContext.setTenantId(TENANT_ID);
            sampleDoc.setCategoryId(5L);
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(sampleDoc));
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            Document result = documentService.setCategory(DOC_ID, null, USER_ID);

            assertThat(result.getCategoryId()).isNull();
        }
    }

    // ==================== setTags ====================

    @Nested
    @DisplayName("setTags")
    class SetTagsTests {

        @Test
        @DisplayName("设置标签成功")
        void setTags_success() {
            TenantContext.setTenantId(TENANT_ID);
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(sampleDoc));
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));

            documentService.setTags(DOC_ID, List.of(1L, 2L), USER_ID);

            verify(tagService).setDocumentTags(DOC_ID, List.of(1L, 2L), TENANT_ID);
        }
    }

    // ==================== listVersions ====================

    @Nested
    @DisplayName("listVersions")
    class ListVersionsTests {

        @Test
        @DisplayName("获取版本列表")
        void listVersions_success() {
            TenantContext.setTenantId(TENANT_ID);
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(sampleDoc));
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            DocumentVersion v1 = DocumentVersion.builder().id(1L).versionNo(1).build();
            DocumentVersion v2 = DocumentVersion.builder().id(2L).versionNo(2).build();
            when(documentVersionRepository.findByDocumentIdOrderByVersionNoDesc(DOC_ID))
                    .thenReturn(List.of(v2, v1));

            List<DocumentVersion> result = documentService.listVersions(DOC_ID, USER_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getVersionNo()).isEqualTo(2);
        }
    }

    // ==================== getDocumentPreview ====================

    @Nested
    @DisplayName("getDocumentPreview")
    class GetDocumentPreviewTests {

        @Test
        @DisplayName("获取预览成功")
        void getDocumentPreview_success() {
            TenantContext.setTenantId(TENANT_ID);
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(sampleDoc));
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));
            DocumentChunk c1 = DocumentChunk.builder().id(1L).chunkIndex(0).content("Part 1").build();
            DocumentChunk c2 = DocumentChunk.builder().id(2L).chunkIndex(1).content("Part 2").build();
            when(chunkRepository.findByTenantIdAndDocumentIdOrderByChunkIndex(TENANT_ID, DOC_ID))
                    .thenReturn(List.of(c1, c2));

            Map<String, Object> result = documentService.getDocumentPreview(DOC_ID, USER_ID);

            assertThat(result.get("documentId")).isEqualTo(DOC_ID);
            assertThat(result.get("documentName")).isEqualTo("test.pdf");
            assertThat(result.get("content")).isEqualTo("Part 1\n\nPart 2");
            assertThat(result.get("chunkCount")).isEqualTo(2);
        }
    }

    // ==================== batchDelete ====================

    @Nested
    @DisplayName("batchDelete")
    class BatchDeleteTests {

        @Test
        @DisplayName("批量删除成功")
        void batchDelete_success() {
            TenantContext.setTenantId(TENANT_ID);
            Document doc1 = Document.builder().id(51L).knowledgeBase(sampleKb).tenantId(TENANT_ID).build();
            Document doc2 = Document.builder().id(52L).knowledgeBase(sampleKb).tenantId(TENANT_ID).build();
            when(documentRepository.findById(51L)).thenReturn(Optional.of(doc1));
            when(documentRepository.findById(52L)).thenReturn(Optional.of(doc2));
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(sampleKb));

            documentService.batchDelete(List.of(51L, 52L), USER_ID);

            verify(documentRepository).delete(doc1);
            verify(documentRepository).delete(doc2);
            verify(knowledgeGraphService).deleteByDocument(51L);
            verify(knowledgeGraphService).deleteByDocument(52L);
        }

        @Test
        @DisplayName("批量删除失败 - 空列表")
        void batchDelete_emptyList() {
            assertThatThrownBy(() -> documentService.batchDelete(List.of(), USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("请选择");
        }

        @Test
        @DisplayName("批量删除失败 - null列表")
        void batchDelete_nullList() {
            assertThatThrownBy(() -> documentService.batchDelete(null, USER_ID))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("批量删除失败 - 文档不存在")
        void batchDelete_docNotFound() {
            when(documentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.batchDelete(List.of(99L), USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(404));
        }

        @Test
        @DisplayName("批量删除失败 - 无权删除他人文档")
        void batchDelete_forbidden() {
            TenantContext.setTenantId(TENANT_ID);
            Document otherDoc = Document.builder().id(51L).knowledgeBase(sampleKb).tenantId(TENANT_ID).build();
            when(documentRepository.findById(51L)).thenReturn(Optional.of(otherDoc));
            KnowledgeBase otherKb = KnowledgeBase.builder().id(KB_ID).userId(2L).build();
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(otherKb));

            assertThatThrownBy(() -> documentService.batchDelete(List.of(51L), USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(403));
        }
    }
}

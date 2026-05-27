package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.model.entity.Document;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import com.zhizhi.ai.repository.DocumentChunkRepository;
import com.zhizhi.ai.repository.DocumentRepository;
import com.zhizhi.ai.repository.KnowledgeBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService 单元测试")
class DocumentServiceTest {

    @InjectMocks
    private DocumentService documentService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository chunkRepository;

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Mock
    private VectorStore vectorStore;

    private KnowledgeBase testKb;
    private final Long KB_ID = 1L;
    private final Long USER_ID = 1L;
    private final Long OTHER_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        testKb = KnowledgeBase.builder()
                .id(KB_ID)
                .userId(USER_ID)
                .name("测试知识库")
                .build();
    }

    @Nested
    @DisplayName("上传文档")
    class UploadDocument {

        @Test
        @DisplayName("上传PDF成功")
        void uploadPdf_success() throws IOException {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "PDF content".getBytes());

            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(testKb));
            when(documentRepository.save(any(Document.class))).thenAnswer(i -> {
                Document doc = i.getArgument(0);
                doc.setId(1L);
                return doc;
            });

            Document result = documentService.uploadDocument(KB_ID, file, USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getFilename()).isEqualTo("test.pdf");
            assertThat(result.getFileType()).isEqualTo("pdf");
            assertThat(result.getStatus()).isEqualTo("processing");
            verify(documentRepository).save(any(Document.class));
        }

        @Test
        @DisplayName("上传TXT成功")
        void uploadTxt_success() throws IOException {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "Text content".getBytes());

            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(testKb));
            when(documentRepository.save(any(Document.class))).thenAnswer(i -> {
                Document doc = i.getArgument(0);
                doc.setId(1L);
                return doc;
            });

            Document result = documentService.uploadDocument(KB_ID, file, USER_ID);

            assertThat(result.getFileType()).isEqualTo("txt");
        }

        @Test
        @DisplayName("上传MD成功")
        void uploadMd_success() throws IOException {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "readme.md", "text/markdown", "# Title".getBytes());

            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(testKb));
            when(documentRepository.save(any(Document.class))).thenAnswer(i -> {
                Document doc = i.getArgument(0);
                doc.setId(1L);
                return doc;
            });

            Document result = documentService.uploadDocument(KB_ID, file, USER_ID);

            assertThat(result.getFileType()).isEqualTo("md");
        }

        @Test
        @DisplayName("不支持的文件格式")
        void upload_unsupportedFormat() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.xlsx", "application/octet-stream", "content".getBytes());

            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(testKb));

            assertThatThrownBy(() -> documentService.uploadDocument(KB_ID, file, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(400));
        }

        @Test
        @DisplayName("知识库不存在")
        void upload_kbNotFound() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "content".getBytes());

            when(knowledgeBaseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.uploadDocument(999L, file, USER_ID))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("无权操作他人知识库")
        void upload_forbidden() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "content".getBytes());

            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(testKb));

            assertThatThrownBy(() -> documentService.uploadDocument(KB_ID, file, OTHER_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(403));
        }
    }

    @Nested
    @DisplayName("文档列表")
    class ListDocuments {

        @Test
        @DisplayName("获取文档列表成功")
        void list_success() {
            Document doc = Document.builder().id(1L).filename("test.pdf").build();
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(testKb));
            when(documentRepository.findByKnowledgeBaseIdOrderByCreatedAtDesc(KB_ID))
                    .thenReturn(List.of(doc));

            List<Document> result = documentService.listByKnowledgeBase(KB_ID, USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFilename()).isEqualTo("test.pdf");
        }

        @Test
        @DisplayName("无权查看")
        void list_forbidden() {
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(testKb));

            assertThatThrownBy(() -> documentService.listByKnowledgeBase(KB_ID, OTHER_USER_ID))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("删除文档")
    class DeleteDocument {

        @Test
        @DisplayName("删除成功")
        void delete_success() {
            Document doc = Document.builder()
                    .id(1L)
                    .knowledgeBase(testKb)
                    .filename("test.pdf")
                    .build();
            when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(testKb));
            doNothing().when(chunkRepository).deleteByDocumentId(1L);

            documentService.deleteDocument(1L, USER_ID);

            verify(chunkRepository).deleteByDocumentId(1L);
            verify(documentRepository).delete(doc);
        }
    }
}

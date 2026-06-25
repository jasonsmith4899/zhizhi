package com.zhizhi.ai.controller;

import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.GlobalExceptionHandler;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.config.SecurityConfig;
import com.zhizhi.ai.model.dto.DocumentListDTO;
import com.zhizhi.ai.model.entity.Document;
import com.zhizhi.ai.model.entity.DocumentChunk;
import com.zhizhi.ai.repository.ApiKeyRepository;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.UserRepository;
import com.zhizhi.ai.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@ActiveProfiles("test")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private AuthUtil authUtil;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private ApiKeyRepository apiKeyRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TenantMemberRepository tenantMemberRepository;

    @BeforeEach
    void setUp() {
        when(authUtil.getUserId(any(Authentication.class))).thenReturn(1L);
    }

    private Document buildDocument(Long id, String filename, String status) {
        Document doc = Document.builder()
                .id(id).filename(filename).fileType("pdf").fileSize(1024L)
                .status(status).chunkCount(5)
                .tenantId(1L)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        return doc;
    }

    // ========== upload ==========

    @Test
    @WithMockUser
    void upload_processingStatus_returnsProcessingMessage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "PDF content".getBytes());

        when(documentService.uploadDocument(eq(10L), any(), eq(1L)))
                .thenReturn(buildDocument(1L, "test.pdf", "processing"));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .param("knowledgeBaseId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.filename").value("test.pdf"))
                .andExpect(jsonPath("$.data.status").value("processing"))
                .andExpect(jsonPath("$.data.message").value("文档已上传，正在处理中..."));
    }

    @Test
    @WithMockUser
    void upload_readyStatus_returnsExistMessage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "existing.pdf", "application/pdf", "PDF content".getBytes());

        when(documentService.uploadDocument(eq(10L), any(), eq(1L)))
                .thenReturn(buildDocument(2L, "existing.pdf", "ready"));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .param("knowledgeBaseId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ready"))
                .andExpect(jsonPath("$.data.message").value("文档已存在，秒传完成"));
    }

    // ========== list ==========

    @Test
    @WithMockUser
    void list_success() throws Exception {
        Document doc1 = buildDocument(1L, "doc1.pdf", "ready");
        Document doc2 = buildDocument(2L, "doc2.txt", "processing");

        when(documentService.listByKnowledgeBase(10L, 1L)).thenReturn(List.of(doc1, doc2));

        mockMvc.perform(get("/api/v1/documents")
                        .param("knowledgeBaseId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].filename").value("doc1.pdf"))
                .andExpect(jsonPath("$.data[1].filename").value("doc2.txt"));
    }

    @Test
    @WithMockUser
    void list_empty_returnsEmptyArray() throws Exception {
        when(documentService.listByKnowledgeBase(10L, 1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/documents")
                        .param("knowledgeBaseId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== delete ==========

    @Test
    @WithMockUser
    void delete_success() throws Exception {
        doNothing().when(documentService).deleteDocument(1L, 1L);

        mockMvc.perform(delete("/api/v1/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(documentService).deleteDocument(1L, 1L);
    }

    @Test
    @WithMockUser
    void delete_notFound_returns404() throws Exception {
        doThrow(BusinessException.notFound("文档")).when(documentService).deleteDocument(99L, 1L);

        mockMvc.perform(delete("/api/v1/documents/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ========== getChunks ==========

    @Test
    @WithMockUser
    void getChunks_success() throws Exception {
        DocumentChunk chunk = DocumentChunk.builder()
                .id(1L).documentId(1L).knowledgeBaseId(10L).tenantId(1L)
                .chunkIndex(0).content("chunk content")
                .build();

        when(documentService.getDocumentChunks(1L, 1L)).thenReturn(List.of(chunk));

        mockMvc.perform(get("/api/v1/documents/1/chunks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].content").value("chunk content"));
    }

    // ========== vectorStatus ==========

    @Test
    @WithMockUser
    void vectorStatus_success() throws Exception {
        when(documentService.getVectorStatus(1L, 1L)).thenReturn(
                Map.of("totalChunks", 5, "vectorizedChunks", 3));

        mockMvc.perform(get("/api/v1/documents/1/vector-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalChunks").value(5))
                .andExpect(jsonPath("$.data.vectorizedChunks").value(3));
    }

    // ========== reVectorize ==========

    @Test
    @WithMockUser
    void reVectorize_success() throws Exception {
        doNothing().when(documentService).reVectorize(1L, 1L);

        mockMvc.perform(post("/api/v1/documents/1/re-vectorize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.message").value("重新向量化已启动"));
    }

    // ========== batchDelete ==========

    @Test
    @WithMockUser
    void batchDelete_success() throws Exception {
        doNothing().when(documentService).batchDelete(anyList(), eq(1L));

        mockMvc.perform(post("/api/v1/documents/batch-delete")
                        .contentType("application/json")
                        .content("{\"ids\":[1,2,3]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(documentService).batchDelete(List.of(1L, 2L, 3L), 1L);
    }

    // ========== preview ==========

    @Test
    @WithMockUser
    void preview_success() throws Exception {
        when(documentService.getDocumentPreview(1L, 1L)).thenReturn(
                Map.of("id", 1L, "filename", "test.pdf", "content", "parsed text"));

        mockMvc.perform(get("/api/v1/documents/1/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.filename").value("test.pdf"));
    }

    // ========== setCategory ==========

    @Test
    @WithMockUser
    void setCategory_success() throws Exception {
        when(documentService.setCategory(1L, 5L, 1L)).thenReturn(buildDocument(1L, "test.pdf", "ready"));

        mockMvc.perform(put("/api/v1/documents/1/category")
                        .contentType("application/json")
                        .content("{\"categoryId\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ========== setTags ==========

    @Test
    @WithMockUser
    void setTags_success() throws Exception {
        doNothing().when(documentService).setTags(eq(1L), anyList(), eq(1L));

        mockMvc.perform(put("/api/v1/documents/1/tags")
                        .contentType("application/json")
                        .content("{\"tagIds\":[1,2,3]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ========== getTags ==========

    @Test
    @WithMockUser
    void getTags_success() throws Exception {
        when(documentService.getDocumentTagIds(1L, 1L)).thenReturn(List.of(1L, 2L, 3L));

        mockMvc.perform(get("/api/v1/documents/1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    // ========== versions ==========

    @Test
    @WithMockUser
    void versions_success() throws Exception {
        when(documentService.listVersions(1L, 1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/documents/1/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ========== rollback ==========

    @Test
    @WithMockUser
    void rollback_success() throws Exception {
        doNothing().when(documentService).rollback(1L, 2, 1L);

        mockMvc.perform(post("/api/v1/documents/1/rollback/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(documentService).rollback(1L, 2, 1L);
    }
}

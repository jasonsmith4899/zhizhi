package com.zhizhi.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.GlobalExceptionHandler;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.config.SecurityConfig;
import com.zhizhi.ai.model.dto.KnowledgeBaseRequest;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import com.zhizhi.ai.repository.ApiKeyRepository;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.UserRepository;
import com.zhizhi.ai.service.KnowledgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KnowledgeController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@ActiveProfiles("test")
class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private KnowledgeService knowledgeService;

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

    private KnowledgeBase buildKb(Long id, String name) {
        return KnowledgeBase.builder()
                .id(id).userId(1L).tenantId(1L)
                .name(name).description("desc")
                .status("active").documentCount(0).chunkCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ========== create ==========

    @Test
    @WithMockUser
    void create_success() throws Exception {
        KnowledgeBaseRequest request = new KnowledgeBaseRequest();
        request.setName("My KB");
        request.setDescription("A test knowledge base");

        when(knowledgeService.create(any(KnowledgeBaseRequest.class), anyLong()))
                .thenReturn(buildKb(1L, "My KB"));

        mockMvc.perform(post("/api/v1/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("My KB"));
    }

    @Test
    @WithMockUser
    void create_blankName_returns400() throws Exception {
        KnowledgeBaseRequest request = new KnowledgeBaseRequest();
        request.setName("");

        mockMvc.perform(post("/api/v1/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ========== list ==========

    @Test
    @WithMockUser
    void list_success() throws Exception {
        when(knowledgeService.listByUser(1L)).thenReturn(List.of(
                buildKb(1L, "KB One"),
                buildKb(2L, "KB Two")
        ));

        mockMvc.perform(get("/api/v1/knowledge-bases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("KB One"))
                .andExpect(jsonPath("$.data[1].name").value("KB Two"));
    }

    @Test
    @WithMockUser
    void list_empty_returnsEmptyArray() throws Exception {
        when(knowledgeService.listByUser(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/knowledge-bases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== getById ==========

    @Test
    @WithMockUser
    void getById_success() throws Exception {
        when(knowledgeService.getById(1L, 1L)).thenReturn(buildKb(1L, "My KB"));

        mockMvc.perform(get("/api/v1/knowledge-bases/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("My KB"));
    }

    @Test
    @WithMockUser
    void getById_notFound_returns404() throws Exception {
        when(knowledgeService.getById(99L, 1L))
                .thenThrow(BusinessException.notFound("知识库"));

        mockMvc.perform(get("/api/v1/knowledge-bases/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ========== update ==========

    @Test
    @WithMockUser
    void update_success() throws Exception {
        KnowledgeBaseRequest request = new KnowledgeBaseRequest();
        request.setName("Updated Name");
        request.setDescription("Updated desc");

        when(knowledgeService.update(eq(1L), any(KnowledgeBaseRequest.class), eq(1L)))
                .thenReturn(buildKb(1L, "Updated Name"));

        mockMvc.perform(put("/api/v1/knowledge-bases/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Updated Name"));
    }

    // ========== delete ==========

    @Test
    @WithMockUser
    void delete_success() throws Exception {
        doNothing().when(knowledgeService).delete(1L, 1L);

        mockMvc.perform(delete("/api/v1/knowledge-bases/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(knowledgeService).delete(1L, 1L);
    }

    @Test
    @WithMockUser
    void delete_notFound_returns404() throws Exception {
        doThrow(BusinessException.notFound("知识库")).when(knowledgeService).delete(99L, 1L);

        mockMvc.perform(delete("/api/v1/knowledge-bases/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}

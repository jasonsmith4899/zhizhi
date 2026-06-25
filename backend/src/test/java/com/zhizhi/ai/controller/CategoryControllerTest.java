package com.zhizhi.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.GlobalExceptionHandler;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.config.SecurityConfig;
import com.zhizhi.ai.model.dto.CategoryRequest;
import com.zhizhi.ai.model.entity.Category;
import com.zhizhi.ai.repository.ApiKeyRepository;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.UserRepository;
import com.zhizhi.ai.service.CategoryService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

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

    private Category buildCategory(Long id, String name, Long parentId) {
        return Category.builder()
                .id(id).tenantId(1L).knowledgeBaseId(10L)
                .parentId(parentId).name(name).sortOrder(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ========== create ==========

    @Test
    @WithMockUser
    void create_success() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setKnowledgeBaseId(10L);
        request.setName("技术文档");
        request.setSortOrder(1);

        when(categoryService.create(eq(10L), isNull(), eq("技术文档"), eq(1), eq(1L)))
                .thenReturn(buildCategory(1L, "技术文档", null));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("技术文档"));
    }

    @Test
    @WithMockUser
    void create_withParent_success() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setKnowledgeBaseId(10L);
        request.setParentId(1L);
        request.setName("子分类");

        when(categoryService.create(eq(10L), eq(1L), eq("子分类"), isNull(), eq(1L)))
                .thenReturn(buildCategory(2L, "子分类", 1L));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value(1));
    }

    @Test
    @WithMockUser
    void create_blankName_returns400() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setKnowledgeBaseId(10L);
        request.setName("");

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ========== list ==========

    @Test
    @WithMockUser
    void list_success() throws Exception {
        when(categoryService.list(10L, 1L)).thenReturn(List.of(
                buildCategory(1L, "分类A", null),
                buildCategory(2L, "分类B", null)
        ));

        mockMvc.perform(get("/api/v1/categories")
                        .param("knowledgeBaseId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("分类A"))
                .andExpect(jsonPath("$.data[1].name").value("分类B"));
    }

    @Test
    @WithMockUser
    void list_empty_returnsEmptyArray() throws Exception {
        when(categoryService.list(10L, 1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/categories")
                        .param("knowledgeBaseId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== update ==========

    @Test
    @WithMockUser
    void update_success() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("新名称");
        request.setParentId(5L);
        request.setSortOrder(3);

        when(categoryService.update(eq(1L), eq("新名称"), eq(5L), eq(3), eq(1L)))
                .thenReturn(buildCategory(1L, "新名称", 5L));

        mockMvc.perform(put("/api/v1/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("新名称"));
    }

    @Test
    @WithMockUser
    void update_notFound_returns404() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("新名称");

        when(categoryService.update(eq(99L), eq("新名称"), isNull(), isNull(), eq(1L)))
                .thenThrow(BusinessException.notFound("分类"));

        mockMvc.perform(put("/api/v1/categories/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ========== delete ==========

    @Test
    @WithMockUser
    void delete_success() throws Exception {
        doNothing().when(categoryService).delete(1L, 1L);

        mockMvc.perform(delete("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(categoryService).delete(1L, 1L);
    }

    @Test
    @WithMockUser
    void delete_notFound_returns404() throws Exception {
        doThrow(BusinessException.notFound("分类")).when(categoryService).delete(99L, 1L);

        mockMvc.perform(delete("/api/v1/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}

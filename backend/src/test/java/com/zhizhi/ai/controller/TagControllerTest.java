package com.zhizhi.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.GlobalExceptionHandler;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.config.SecurityConfig;
import com.zhizhi.ai.model.dto.TagRequest;
import com.zhizhi.ai.model.entity.Tag;
import com.zhizhi.ai.repository.ApiKeyRepository;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.UserRepository;
import com.zhizhi.ai.service.TagService;
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

@WebMvcTest(TagController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@ActiveProfiles("test")
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TagService tagService;

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

    private Tag buildTag(Long id, String name, String color) {
        return Tag.builder()
                .id(id).tenantId(1L).knowledgeBaseId(10L)
                .name(name).color(color)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ========== create ==========

    @Test
    @WithMockUser
    void create_success() throws Exception {
        TagRequest request = new TagRequest();
        request.setKnowledgeBaseId(10L);
        request.setName("重要");
        request.setColor("#ff0000");

        when(tagService.create(eq(10L), eq("重要"), eq("#ff0000"), eq(1L)))
                .thenReturn(buildTag(1L, "重要", "#ff0000"));

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("重要"))
                .andExpect(jsonPath("$.data.color").value("#ff0000"));
    }

    @Test
    @WithMockUser
    void create_blankName_returns400() throws Exception {
        TagRequest request = new TagRequest();
        request.setKnowledgeBaseId(10L);
        request.setName("");

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser
    void create_withoutColor_success() throws Exception {
        TagRequest request = new TagRequest();
        request.setKnowledgeBaseId(10L);
        request.setName("默认标签");

        when(tagService.create(eq(10L), eq("默认标签"), isNull(), eq(1L)))
                .thenReturn(buildTag(2L, "默认标签", null));

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("默认标签"));
    }

    // ========== list ==========

    @Test
    @WithMockUser
    void list_success() throws Exception {
        when(tagService.list(10L, 1L)).thenReturn(List.of(
                buildTag(1L, "重要", "#ff0000"),
                buildTag(2L, "待办", "#00ff00")
        ));

        mockMvc.perform(get("/api/v1/tags")
                        .param("knowledgeBaseId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("重要"))
                .andExpect(jsonPath("$.data[1].name").value("待办"));
    }

    @Test
    @WithMockUser
    void list_empty_returnsEmptyArray() throws Exception {
        when(tagService.list(10L, 1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tags")
                        .param("knowledgeBaseId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== delete ==========

    @Test
    @WithMockUser
    void delete_success() throws Exception {
        doNothing().when(tagService).delete(1L, 1L);

        mockMvc.perform(delete("/api/v1/tags/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tagService).delete(1L, 1L);
    }

    @Test
    @WithMockUser
    void delete_notFound_returns404() throws Exception {
        doThrow(BusinessException.notFound("标签")).when(tagService).delete(99L, 1L);

        mockMvc.perform(delete("/api/v1/tags/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}

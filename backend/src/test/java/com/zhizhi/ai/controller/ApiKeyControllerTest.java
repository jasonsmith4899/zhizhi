package com.zhizhi.ai.controller;

import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.GlobalExceptionHandler;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.config.SecurityConfig;
import com.zhizhi.ai.model.dto.ApiKeyDTO;
import com.zhizhi.ai.repository.ApiKeyRepository;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.UserRepository;
import com.zhizhi.ai.service.AuthService;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiKeyController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@ActiveProfiles("test")
class ApiKeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

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

    private ApiKeyDTO buildApiKeyDTO(Long id, String name) {
        return ApiKeyDTO.builder()
                .id(id).name(name).description("test key")
                .keyValue("sk-1234****5678")
                .assistantPersona("You are helpful")
                .createdAt(LocalDateTime.now())
                .knowledgeBaseIds(Set.of(1L, 2L))
                .build();
    }

    // ========== list ==========

    @Test
    @WithMockUser
    void list_success() throws Exception {
        when(authService.listApiKeys(1L)).thenReturn(List.of(
                buildApiKeyDTO(1L, "Key One"),
                buildApiKeyDTO(2L, "Key Two")
        ));

        mockMvc.perform(get("/api/v1/api-keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Key One"))
                .andExpect(jsonPath("$.data[1].name").value("Key Two"));
    }

    @Test
    @WithMockUser
    void list_empty_returnsEmptyArray() throws Exception {
        when(authService.listApiKeys(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/api-keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== create ==========

    @Test
    @WithMockUser
    void create_success() throws Exception {
        ApiKeyDTO created = ApiKeyDTO.builder()
                .id(1L).name("新Key").description("desc")
                .keyValue("sk-abcd1234efgh5678")
                .createdAt(LocalDateTime.now())
                .knowledgeBaseIds(Set.of(1L))
                .build();

        when(authService.createApiKey(
                eq(1L), eq("新Key"), eq("desc"),
                eq("persona"), eq("background"), eq("rules"),
                anySet()))
                .thenReturn(created);

        mockMvc.perform(post("/api/v1/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新Key\",\"description\":\"desc\","
                                + "\"assistantPersona\":\"persona\","
                                + "\"merchantBackground\":\"background\","
                                + "\"answerRules\":\"rules\","
                                + "\"knowledgeBaseIds\":[1]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("新Key"))
                .andExpect(jsonPath("$.data.keyValue").value("sk-abcd1234efgh5678"));
    }

    @Test
    @WithMockUser
    void create_defaultName_usesDefault() throws Exception {
        ApiKeyDTO created = buildApiKeyDTO(1L, "默认Key");

        when(authService.createApiKey(
                eq(1L), eq("默认Key"), isNull(),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(created);

        mockMvc.perform(post("/api/v1/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("默认Key"));
    }

    // ========== update ==========

    @Test
    @WithMockUser
    void update_success() throws Exception {
        ApiKeyDTO updated = buildApiKeyDTO(1L, "更新后的Key");

        when(authService.updateApiKey(
                eq(1L), eq(1L), eq("更新后的Key"), eq("new desc"),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/api-keys/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"更新后的Key\",\"description\":\"new desc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("更新后的Key"));
    }

    @Test
    @WithMockUser
    void update_notFound_returns404() throws Exception {
        when(authService.updateApiKey(
                eq(1L), eq(99L), any(), any(), any(), any(), any(), any()))
                .thenThrow(BusinessException.notFound("API Key"));

        mockMvc.perform(put("/api/v1/api-keys/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ========== delete ==========

    @Test
    @WithMockUser
    void delete_success() throws Exception {
        doNothing().when(authService).deleteApiKey(1L, 1L);

        mockMvc.perform(delete("/api/v1/api-keys/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(authService).deleteApiKey(1L, 1L);
    }

    @Test
    @WithMockUser
    void delete_notFound_returns404() throws Exception {
        doThrow(BusinessException.notFound("API Key")).when(authService).deleteApiKey(1L, 99L);

        mockMvc.perform(delete("/api/v1/api-keys/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}

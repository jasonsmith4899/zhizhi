package com.zhizhi.ai.controller;

import com.zhizhi.ai.common.GlobalExceptionHandler;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.config.SecurityConfig;
import com.zhizhi.ai.model.entity.AuditLog;
import com.zhizhi.ai.repository.ApiKeyRepository;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.UserRepository;
import com.zhizhi.ai.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@ActiveProfiles("test")
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private ApiKeyRepository apiKeyRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TenantMemberRepository tenantMemberRepository;

    @Test
    @WithMockUser
    void list_returnsPagedAuditLogs() throws Exception {
        AuditLog log1 = AuditLog.builder()
                .id(1L).tenantId(1L).userId(1L).action("CREATE")
                .targetType("knowledge_base").targetId(10L)
                .ip("127.0.0.1").success(true)
                .createdAt(LocalDateTime.now())
                .build();

        Page<AuditLog> page = new PageImpl<>(List.of(log1), PageRequest.of(0, 20), 1);
        when(auditService.list(0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].action").value("CREATE"))
                .andExpect(jsonPath("$.data.content[0].targetType").value("knowledge_base"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser
    void list_withCustomPageParams_passesToService() throws Exception {
        Page<AuditLog> page = new PageImpl<>(List.of(), PageRequest.of(2, 5), 0);
        when(auditService.list(2, 5)).thenReturn(page);

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @WithMockUser
    void list_defaultParams_usesZeroAndTwenty() throws Exception {
        Page<AuditLog> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(auditService.list(0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}

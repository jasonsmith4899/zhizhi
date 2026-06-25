package com.zhizhi.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.GlobalExceptionHandler;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.config.SecurityConfig;
import com.zhizhi.ai.model.dto.TenantUpdateRequest;
import com.zhizhi.ai.model.entity.Tenant;
import com.zhizhi.ai.model.entity.TenantMember;
import com.zhizhi.ai.model.entity.User;
import com.zhizhi.ai.repository.ApiKeyRepository;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.UserRepository;
import com.zhizhi.ai.service.StatsService;
import com.zhizhi.ai.service.TenantService;
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
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@ActiveProfiles("test")
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private StatsService statsService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthUtil authUtil;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private ApiKeyRepository apiKeyRepository;

    @MockitoBean
    private TenantMemberRepository tenantMemberRepository;

    @BeforeEach
    void setUp() {
        when(authUtil.getUserId(any(Authentication.class))).thenReturn(1L);
    }

    private Tenant buildTenant(Long id, String name) {
        return Tenant.builder()
                .id(id).name(name).plan("free").status("active")
                .maxDocuments(10).maxDailyQueries(100)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ========== create ==========

    @Test
    @WithMockUser
    void create_success() throws Exception {
        when(tenantService.create("新租户", 1L)).thenReturn(buildTenant(1L, "新租户"));

        mockMvc.perform(post("/api/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新租户\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("新租户"))
                .andExpect(jsonPath("$.data.plan").value("free"));
    }

    @Test
    @WithMockUser
    void create_serviceThrows_propagates() throws Exception {
        when(tenantService.create("新租户", 1L))
                .thenThrow(BusinessException.badRequest("用户已有租户"));

        mockMvc.perform(post("/api/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新租户\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ========== getMyTenant ==========

    @Test
    @WithMockUser
    void getMyTenant_success() throws Exception {
        when(tenantService.getByUserId(1L)).thenReturn(buildTenant(1L, "My Tenant"));

        mockMvc.perform(get("/api/v1/tenants/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("My Tenant"));
    }

    @Test
    @WithMockUser
    void getMyTenant_notFound_returns404() throws Exception {
        when(tenantService.getByUserId(1L))
                .thenThrow(BusinessException.notFound("租户"));

        mockMvc.perform(get("/api/v1/tenants/mine"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ========== getById ==========

    @Test
    @WithMockUser
    void getById_success() throws Exception {
        when(tenantService.isMember(1L, 1L)).thenReturn(true);
        when(tenantService.getById(1L)).thenReturn(buildTenant(1L, "Tenant"));

        mockMvc.perform(get("/api/v1/tenants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser
    void getById_notMember_returns403() throws Exception {
        when(tenantService.isMember(1L, 1L)).thenReturn(false);

        mockMvc.perform(get("/api/v1/tenants/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    // ========== update ==========

    @Test
    @WithMockUser
    void update_success() throws Exception {
        Tenant updated = buildTenant(1L, "Updated");
        when(tenantService.update(eq(1L), any(TenantUpdateRequest.class), eq(1L)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/tenants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Updated"));
    }

    @Test
    @WithMockUser
    void update_noPermission_returns403() throws Exception {
        when(tenantService.update(eq(1L), any(), eq(1L)))
                .thenThrow(BusinessException.forbidden("无权操作"));

        mockMvc.perform(put("/api/v1/tenants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    // ========== addMember ==========

    @Test
    @WithMockUser
    void addMember_success() throws Exception {
        TenantMember member = TenantMember.builder()
                .id(1L).tenantId(1L).userId(2L).role("member")
                .createdAt(LocalDateTime.now())
                .build();
        when(tenantService.addMember(eq(1L), eq("bob"), eq("member"), eq(1L)))
                .thenReturn(member);
        when(userRepository.findById(2L)).thenReturn(
                Optional.of(User.builder().id(2L).username("bob").build()));

        mockMvc.perform(post("/api/v1/tenants/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("bob"))
                .andExpect(jsonPath("$.data.role").value("member"));
    }

    @Test
    @WithMockUser
    void addMember_withRole_success() throws Exception {
        TenantMember member = TenantMember.builder()
                .id(1L).tenantId(1L).userId(2L).role("admin")
                .createdAt(LocalDateTime.now())
                .build();
        when(tenantService.addMember(eq(1L), eq("bob"), eq("admin"), eq(1L)))
                .thenReturn(member);
        when(userRepository.findById(2L)).thenReturn(
                Optional.of(User.builder().id(2L).username("bob").build()));

        mockMvc.perform(post("/api/v1/tenants/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"role\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("admin"));
    }

    @Test
    @WithMockUser
    void addMember_userNotFound_returns404() throws Exception {
        when(tenantService.addMember(eq(1L), eq("ghost"), eq("member"), eq(1L)))
                .thenThrow(BusinessException.notFound("用户"));

        mockMvc.perform(post("/api/v1/tenants/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ghost\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ========== getMembers ==========

    @Test
    @WithMockUser
    void getMembers_success() throws Exception {
        TenantMember m1 = TenantMember.builder()
                .id(1L).tenantId(1L).userId(1L).role("owner").createdAt(LocalDateTime.now()).build();
        TenantMember m2 = TenantMember.builder()
                .id(2L).tenantId(1L).userId(2L).role("member").createdAt(LocalDateTime.now()).build();

        when(tenantService.getMembers(1L, 1L)).thenReturn(List.of(m1, m2));
        when(userRepository.findById(1L)).thenReturn(
                Optional.of(User.builder().id(1L).username("admin").build()));
        when(userRepository.findById(2L)).thenReturn(
                Optional.of(User.builder().id(2L).username("bob").build()));

        mockMvc.perform(get("/api/v1/tenants/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].username").value("admin"))
                .andExpect(jsonPath("$.data[1].username").value("bob"));
    }

    @Test
    @WithMockUser
    void getMembers_noPermission_returns403() throws Exception {
        when(tenantService.getMembers(1L, 1L))
                .thenThrow(BusinessException.forbidden("无权操作"));

        mockMvc.perform(get("/api/v1/tenants/1/members"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    // ========== dashboard ==========

    @Test
    @WithMockUser
    void dashboard_success() throws Exception {
        when(tenantService.isMember(1L, 1L)).thenReturn(true);
        when(statsService.getDashboard(1L)).thenReturn(
                Map.of("totalDocuments", 10, "totalQueries", 50));

        mockMvc.perform(get("/api/v1/tenants/1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalDocuments").value(10))
                .andExpect(jsonPath("$.data.totalQueries").value(50));
    }

    @Test
    @WithMockUser
    void dashboard_notMember_returns403() throws Exception {
        when(tenantService.isMember(1L, 1L)).thenReturn(false);

        mockMvc.perform(get("/api/v1/tenants/1/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}

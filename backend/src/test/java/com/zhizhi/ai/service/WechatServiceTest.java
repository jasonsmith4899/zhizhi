package com.zhizhi.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.model.entity.Tenant;
import com.zhizhi.ai.model.entity.TenantMember;
import com.zhizhi.ai.model.entity.User;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.TenantRepository;
import com.zhizhi.ai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WechatServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantMemberRepository tenantMemberRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RestTemplate restTemplate;

    private WechatService wechatService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        wechatService = new WechatService(
                userRepository, tenantRepository, tenantMemberRepository,
                jwtUtil, objectMapper, restTemplate);
        ReflectionTestUtils.setField(wechatService, "defaultAppId", "wx_default_appid");
        ReflectionTestUtils.setField(wechatService, "defaultSecret", "wx_default_secret");
    }

    // ---------- miniProgramLogin: happy paths ----------

    @Test
    void miniProgramLogin_existingUser_withMembership() {
        // WeChat API returns openid
        String wechatResponse = "{\"openid\":\"openid123\",\"session_key\":\"sk\"}";
        when(restTemplate.getForObject(contains("jscode2session"), eq(String.class)))
                .thenReturn(wechatResponse);

        // Existing user found
        User existingUser = User.builder().id(1L).username("wx_openid123").build();
        when(userRepository.findByUsername("wx_openid123")).thenReturn(Optional.of(existingUser));

        // Already has tenant membership
        TenantMember existingMember = TenantMember.builder().tenantId(10L).userId(1L).role("member").build();
        when(tenantMemberRepository.findByUserId(1L)).thenReturn(Optional.of(existingMember));

        // JWT
        when(jwtUtil.generateToken(1L, "wx_openid123", 10L)).thenReturn("jwt-token-abc");

        Map<String, Object> result = wechatService.miniProgramLogin("code123", null);

        assertEquals("jwt-token-abc", result.get("token"));
        assertEquals(1L, result.get("userId"));
        assertEquals(10L, result.get("tenantId"));
        assertFalse((Boolean) result.get("isNew"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void miniProgramLogin_newUser_autoCreated() {
        String wechatResponse = "{\"openid\":\"new_openid\",\"session_key\":\"sk\"}";
        when(restTemplate.getForObject(contains("jscode2session"), eq(String.class)))
                .thenReturn(wechatResponse);

        // User not found
        when(userRepository.findByUsername("wx_new_openid")).thenReturn(Optional.empty());

        // Create user
        User newUser = User.builder().id(5L).username("wx_new_openid").build();
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // No existing membership, tenantId provided
        when(tenantMemberRepository.findByUserId(5L)).thenReturn(Optional.empty());
        Tenant tenant = Tenant.builder().id(10L).build();
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(tenant));
        when(tenantMemberRepository.save(any(TenantMember.class))).thenReturn(null);

        when(jwtUtil.generateToken(5L, "wx_new_openid", 10L)).thenReturn("jwt-new-token");

        Map<String, Object> result = wechatService.miniProgramLogin("code_new", 10L);

        assertEquals("jwt-new-token", result.get("token"));
        assertEquals(5L, result.get("userId"));
        assertEquals(10L, result.get("tenantId"));
        assertTrue((Boolean) result.get("isNew"));

        // Verify user was created with correct fields
        verify(userRepository).save(argThat(user ->
                user.getUsername().equals("wx_new_openid") &&
                user.getEmail().equals("wx_new_openid@wechat.local") &&
                "free".equals(user.getPlan())
        ));

        // Verify tenant member created with "member" role
        verify(tenantMemberRepository).save(argThat(m ->
                m.getTenantId().equals(10L) &&
                m.getUserId().equals(5L) &&
                "member".equals(m.getRole())
        ));
    }

    @Test
    void miniProgramLogin_tenantConfiguredAppId() {
        String wechatResponse = "{\"openid\":\"oid_tenant\",\"session_key\":\"sk\"}";
        when(restTemplate.getForObject(contains("jscode2session"), eq(String.class)))
                .thenReturn(wechatResponse);

        Tenant tenant = Tenant.builder().id(20L).wechatAppid("wx_tenant_app").wechatSecret("tenant_secret").build();
        when(tenantRepository.findById(20L)).thenReturn(Optional.of(tenant));

        User user = User.builder().id(1L).username("wx_oid_tenant").build();
        when(userRepository.findByUsername("wx_oid_tenant")).thenReturn(Optional.of(user));
        when(tenantMemberRepository.findByUserId(1L)).thenReturn(Optional.of(
                TenantMember.builder().tenantId(20L).userId(1L).build()));
        when(jwtUtil.generateToken(1L, "wx_oid_tenant", 20L)).thenReturn("jwt-tenant");

        Map<String, Object> result = wechatService.miniProgramLogin("code_tenant", 20L);

        assertEquals("jwt-tenant", result.get("token"));
        assertEquals(20L, result.get("tenantId"));

        // Verify the tenant-specific appid was used in the URL
        verify(restTemplate).getForObject(contains("wx_tenant_app"), eq(String.class));
    }

    // ---------- miniProgramLogin: error cases ----------

    @Test
    void miniProgramLogin_wechatErrorcode_throws() {
        String wechatResponse = "{\"errcode\":40029,\"errmsg\":\"invalid code\"}";
        when(restTemplate.getForObject(contains("jscode2session"), eq(String.class)))
                .thenReturn(wechatResponse);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> wechatService.miniProgramLogin("bad_code", null));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("微信登录失败"));
        assertTrue(ex.getMessage().contains("invalid code"));
    }

    @Test
    void miniProgramLogin_wechatErrorcodeZero_noError() {
        // errcode=0 means success
        String wechatResponse = "{\"errcode\":0,\"errmsg\":\"ok\",\"openid\":\"ok_openid\",\"session_key\":\"sk\"}";
        when(restTemplate.getForObject(contains("jscode2session"), eq(String.class)))
                .thenReturn(wechatResponse);

        User user = User.builder().id(1L).username("wx_ok_openid").build();
        when(userRepository.findByUsername("wx_ok_openid")).thenReturn(Optional.of(user));
        when(tenantMemberRepository.findByUserId(1L)).thenReturn(Optional.of(
                TenantMember.builder().tenantId(10L).userId(1L).build()));
        when(jwtUtil.generateToken(1L, "wx_ok_openid", 10L)).thenReturn("jwt-ok");

        Map<String, Object> result = wechatService.miniProgramLogin("ok_code", null);
        assertEquals("jwt-ok", result.get("token"));
    }

    @Test
    void miniProgramLogin_restTemplateException_throws() {
        when(restTemplate.getForObject(contains("jscode2session"), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> wechatService.miniProgramLogin("code", null));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("微信登录服务异常"));
    }

    @Test
    void miniProgramLogin_noAppId_throws() {
        ReflectionTestUtils.setField(wechatService, "defaultAppId", "");

        // No tenantId → uses default appId, which is empty
        BusinessException ex = assertThrows(BusinessException.class,
                () -> wechatService.miniProgramLogin("code", null));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("AppID"));
    }

    @Test
    void miniProgramLogin_nullAppId_throws() {
        ReflectionTestUtils.setField(wechatService, "defaultAppId", null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> wechatService.miniProgramLogin("code", null));
        assertEquals(400, ex.getCode());
    }

    @Test
    void miniProgramLogin_tenantNotFound_usesDefault() {
        String wechatResponse = "{\"openid\":\"oid_default\",\"session_key\":\"sk\"}";
        when(restTemplate.getForObject(contains("jscode2session"), eq(String.class)))
                .thenReturn(wechatResponse);

        // Tenant not found → falls back to default appId
        when(tenantRepository.findById(999L)).thenReturn(Optional.empty());

        User user = User.builder().id(1L).username("wx_oid_default").build();
        when(userRepository.findByUsername("wx_oid_default")).thenReturn(Optional.of(user));
        when(tenantMemberRepository.findByUserId(1L)).thenReturn(Optional.of(
                TenantMember.builder().tenantId(10L).userId(1L).build()));
        when(jwtUtil.generateToken(1L, "wx_oid_default", 10L)).thenReturn("jwt-fallback");

        Map<String, Object> result = wechatService.miniProgramLogin("code", 999L);
        assertEquals("jwt-fallback", result.get("token"));

        // Used default appid in URL
        verify(restTemplate).getForObject(contains("wx_default_appid"), eq(String.class));
    }

    // ---------- ensureTenantMember edge cases ----------

    @Test
    void miniProgramLogin_noMembership_noTenantId_throws() {
        String wechatResponse = "{\"openid\":\"oid_no_tenant\",\"session_key\":\"sk\"}";
        when(restTemplate.getForObject(contains("jscode2session"), eq(String.class)))
                .thenReturn(wechatResponse);

        User user = User.builder().id(1L).username("wx_oid_no_tenant").build();
        when(userRepository.findByUsername("wx_oid_no_tenant")).thenReturn(Optional.of(user));

        // No existing membership
        when(tenantMemberRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // No tenantId provided
        BusinessException ex = assertThrows(BusinessException.class,
                () -> wechatService.miniProgramLogin("code", null));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("未绑定租户"));
    }

    @Test
    void miniProgramLogin_noMembership_tenantNotFound_throws() {
        String wechatResponse = "{\"openid\":\"oid_bad_tenant\",\"session_key\":\"sk\"}";
        when(restTemplate.getForObject(contains("jscode2session"), eq(String.class)))
                .thenReturn(wechatResponse);

        User user = User.builder().id(1L).username("wx_oid_bad_tenant").build();
        when(userRepository.findByUsername("wx_oid_bad_tenant")).thenReturn(Optional.of(user));

        when(tenantMemberRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(tenantRepository.findById(55L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> wechatService.miniProgramLogin("code", 55L));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("租户不存在"));
    }

    // ---------- code2session: error response without errmsg ----------

    @Test
    void miniProgramLogin_wechatErrorWithoutErrmsg() {
        String wechatResponse = "{\"errcode\":40163}";
        when(restTemplate.getForObject(contains("jscode2session"), eq(String.class)))
                .thenReturn(wechatResponse);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> wechatService.miniProgramLogin("code", null));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("微信登录失败"));
        assertTrue(ex.getMessage().contains("未知错误"));
    }

    // ---------- code2session: malformed response ----------

    @Test
    void miniProgramLogin_malformedJson_throws() {
        when(restTemplate.getForObject(contains("jscode2session"), eq(String.class)))
                .thenReturn("not-json");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> wechatService.miniProgramLogin("code", null));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("微信登录服务异常"));
    }

    @Test
    void miniProgramLogin_responseMissingOpenid_throws() {
        String wechatResponse = "{\"session_key\":\"sk_only\"}";
        when(restTemplate.getForObject(contains("jscode2session"), eq(String.class)))
                .thenReturn(wechatResponse);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> wechatService.miniProgramLogin("code", null));
        assertEquals(400, ex.getCode());
    }
}

package com.zhizhi.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.entity.TenantMember;
import com.zhizhi.ai.repository.TenantMemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantInterceptor 单元测试")
class TenantInterceptorTest {

    @Mock
    private TenantMemberRepository tenantMemberRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private TenantInterceptor interceptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        interceptor = new TenantInterceptor(tenantMemberRepository, objectMapper);
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("未认证请求直接放行返回 true")
    void preHandle_noAuth_returnsTrue() throws Exception {
        SecurityContextHolder.clearContext();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    @Test
    @DisplayName("认证但 details 不是 Long 类型直接放行")
    void preHandle_detailsNotLong_returnsTrue() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null, java.util.List.of());
        auth.setDetails("not-a-long");
        SecurityContextHolder.getContext().setAuthentication(auth);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    @Test
    @DisplayName("用户未关联租户返回 403")
    void preHandle_noTenantMember_returns403() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null, java.util.List.of());
        auth.setDetails(10L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(tenantMemberRepository.findByUserId(10L)).thenReturn(Optional.empty());
        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).setStatus(403);
        verify(response).setContentType("application/json;charset=UTF-8");
        assertTrue(stringWriter.toString().contains("用户未关联租户"));
    }

    @Test
    @DisplayName("用户有租户成员关系设置 TenantContext 并放行")
    void preHandle_hasTenantMember_setsContextAndReturnsTrue() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null, java.util.List.of());
        auth.setDetails(10L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        TenantMember member = TenantMember.builder()
                .id(1L)
                .tenantId(20L)
                .userId(10L)
                .role("member")
                .build();
        when(tenantMemberRepository.findByUserId(10L)).thenReturn(Optional.of(member));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(20L, TenantContext.getTenantId());
    }

    @Test
    @DisplayName("afterCompletion 清除 TenantContext")
    void afterCompletion_clearsContext() {
        TenantContext.setTenantId(99L);

        interceptor.afterCompletion(request, response, new Object(), null);

        assertNull(TenantContext.getTenantId());
    }
}

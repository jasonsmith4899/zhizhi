package com.zhizhi.ai.config;

import com.zhizhi.ai.common.Result;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.entity.TenantMember;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户拦截器 - 从已认证用户的租户成员关系中提取tenantId
 */
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantMemberRepository tenantMemberRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 未认证请求直接放行是安全的：Spring Security在本拦截器之前已拦截未认证请求（除permitAll路径外）。
        // permitAll路径（登录、注册、健康检查）不需要租户信息。
        if (authentication == null || !authentication.isAuthenticated()) {
            return true;
        }

        Object details = authentication.getDetails();
        if (!(details instanceof Long userId)) {
            return true;
        }

        TenantMember member = tenantMemberRepository.findByUserId(userId).orElse(null);
        if (member == null) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.error(403, "用户未关联租户")));
            return false;
        }

        TenantContext.setTenantId(member.getTenantId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }
}

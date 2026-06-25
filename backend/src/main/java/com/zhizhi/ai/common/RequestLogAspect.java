package com.zhizhi.ai.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.StringJoiner;

/**
 * 请求日志切面：拦截所有 @RestController 方法。
 * - 异常时记录接口现场（method/URI/入参/userId/IP/异常类型，不打堆栈，warn 级别）后重新抛出；
 * - 正常请求耗时超阈值记慢请求（warn）；
 * - 对话端点（POST /api/v1/chat、/api/v1/chat/stream）跳过记录。
 * 堆栈由 GlobalExceptionHandler 负责，避免重复。
 */
@Slf4j
@Aspect
@Component
public class RequestLogAspect {

    @Value("${app.log.slow-request-ms:2000}")
    private long slowRequestMs;

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest request = currentRequest();
        String method = request != null ? request.getMethod() : "?";
        String uri = request != null ? request.getServletPath() : "?";

        if (isChatEndpoint(method, uri)) {
            return pjp.proceed();
        }

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long cost = System.currentTimeMillis() - start;
            if (cost > slowRequestMs) {
                log.warn("慢请求 {}ms: {} {} | userId={} ip={}",
                        cost, method, uri, currentUserId(), currentIp(request));
            }
            return result;
        } catch (Throwable t) {
            log.warn("接口异常现场: {} {} | 入参={} | userId={} ip={} | 异常={}: {}",
                    method, uri, summarize(pjp.getArgs()), currentUserId(), currentIp(request),
                    t.getClass().getSimpleName(), t.getMessage());
            throw t;
        }
    }

    private boolean isChatEndpoint(String method, String uri) {
        return "POST".equals(method)
                && ("/api/v1/chat".equals(uri) || "/api/v1/chat/stream".equals(uri));
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Long currentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getDetails() instanceof Long uid) {
                return uid;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String currentIp(HttpServletRequest request) {
        return request != null ? request.getRemoteAddr() : null;
    }

    private String summarize(Object[] args) {
        if (args == null) return "";
        StringJoiner sj = new StringJoiner(", ");
        for (Object arg : args) {
            if (arg == null) continue;
            if (arg instanceof Authentication || arg instanceof MultipartFile) continue;
            sj.add(String.valueOf(arg));
        }
        return sj.toString();
    }
}

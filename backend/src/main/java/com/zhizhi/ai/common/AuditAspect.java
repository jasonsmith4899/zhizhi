package com.zhizhi.ai.common;

import com.zhizhi.ai.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.StringJoiner;

/**
 * 审计切面：拦截 {@link Auditable} 方法，在原线程采集上下文后异步落库。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final AuthUtil authUtil;

    @Around("@annotation(auditable)")
    public Object around(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        // 在当前（请求）线程采集上下文，@Async 落库时 ThreadLocal 已不可用
        Long tenantId = TenantContext.getTenantId();
        Long userId = currentUserId();
        String ip = currentIp();
        Long targetId = extractTargetId(pjp.getArgs());
        String detail = summarize(pjp.getArgs());

        boolean success = true;
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            success = false;
            throw t;
        } finally {
            try {
                auditService.record(tenantId, userId, auditable.action(),
                        auditable.targetType(), targetId, detail, ip, success);
            } catch (Exception e) {
                log.warn("审计提交失败: {}", e.getMessage());
            }
        }
    }

    private Long currentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? authUtil.getUserId(auth) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String currentIp() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest().getRemoteAddr() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 启发式：取第一个 Long 参数作为目标 ID（通常为 @PathVariable id） */
    private Long extractTargetId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long l) return l;
        }
        return null;
    }

    /** 拼接可读参数摘要，跳过认证对象与文件 */
    private String summarize(Object[] args) {
        StringJoiner sj = new StringJoiner(", ");
        for (Object arg : args) {
            if (arg == null) continue;
            if (arg instanceof Authentication || arg instanceof MultipartFile) continue;
            sj.add(String.valueOf(arg));
        }
        return sj.toString();
    }
}

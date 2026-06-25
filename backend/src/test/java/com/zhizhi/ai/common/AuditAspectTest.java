package com.zhizhi.ai.common;

import com.zhizhi.ai.service.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuditAspect 审计切面测试")
class AuditAspectTest {

    @Mock
    private AuditService auditService;

    @Mock
    private AuthUtil authUtil;

    @InjectMocks
    private AuditAspect auditAspect;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private Auditable buildAuditable(String action, String targetType) {
        return new Auditable() {
            @Override
            public String action() { return action; }
            @Override
            public String targetType() { return targetType; }
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return Auditable.class; }
        };
    }

    @Test
    @DisplayName("方法正常执行时记录 success=true 的审计日志")
    void around_success_recordsAuditLog() throws Throwable {
        TenantContext.setTenantId(10L);

        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(authUtil.getUserId(auth)).thenReturn(1L);

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getArgs()).thenReturn(new Object[]{100L, "param1"});
        when(pjp.proceed()).thenReturn("result");

        Auditable auditable = buildAuditable("CREATE", "document");

        Object result = auditAspect.around(pjp, auditable);

        assertEquals("result", result);
        verify(auditService).record(eq(10L), eq(1L), eq("CREATE"), eq("document"),
                eq(100L), eq("100, param1"), isNull(), eq(true));
    }

    @Test
    @DisplayName("方法抛异常时记录 success=false 并重新抛出")
    void around_failure_recordsAuditLogAndRethrows() throws Throwable {
        TenantContext.setTenantId(10L);

        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(authUtil.getUserId(auth)).thenReturn(1L);

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getArgs()).thenReturn(new Object[]{200L});
        RuntimeException cause = new RuntimeException("boom");
        when(pjp.proceed()).thenThrow(cause);

        Auditable auditable = buildAuditable("DELETE", "document");

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> auditAspect.around(pjp, auditable));
        assertSame(cause, thrown);

        verify(auditService).record(eq(10L), eq(1L), eq("DELETE"), eq("document"),
                eq(200L), eq("200"), isNull(), eq(false));
    }

    @Test
    @DisplayName("auditService.record 抛异常不影响原方法返回值")
    void around_auditServiceFails_doesNotAffectResult() throws Throwable {
        TenantContext.setTenantId(10L);

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getArgs()).thenReturn(new Object[0]);
        when(pjp.proceed()).thenReturn("ok");

        doThrow(new RuntimeException("db error")).when(auditService)
                .record(any(), any(), anyString(), anyString(), any(), anyString(), any(), anyBoolean());

        Auditable auditable = buildAuditable("UPDATE", "tag");

        Object result = auditAspect.around(pjp, auditable);
        assertEquals("ok", result);
    }

    @Test
    @DisplayName("无认证信息时 userId 为 null")
    void around_noAuth_userIdNull() throws Throwable {
        TenantContext.setTenantId(10L);
        SecurityContextHolder.clearContext();

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getArgs()).thenReturn(new Object[0]);
        when(pjp.proceed()).thenReturn("ok");

        Auditable auditable = buildAuditable("CREATE", "kb");

        auditAspect.around(pjp, auditable);

        verify(auditService).record(eq(10L), isNull(), eq("CREATE"), eq("kb"),
                isNull(), eq(""), isNull(), eq(true));
    }

    @Test
    @DisplayName("summarize 跳过 Authentication 和 MultipartFile 参数")
    void around_skipsAuthAndFileInSummary() throws Throwable {
        TenantContext.setTenantId(1L);

        Authentication authParam = mock(Authentication.class);
        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getArgs()).thenReturn(new Object[]{10L, authParam, file, "extra"});
        when(pjp.proceed()).thenReturn("ok");

        Auditable auditable = buildAuditable("CREATE", "document");

        auditAspect.around(pjp, auditable);

        // summary 应只包含 10 和 extra，跳过 auth 和 file
        verify(auditService).record(any(), any(), anyString(), anyString(),
                eq(10L), eq("10, extra"), any(), anyBoolean());
    }
}

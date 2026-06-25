package com.zhizhi.ai.common;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitAspect 限流切面测试")
class RateLimitAspectTest {

    private RateLimitAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RateLimitAspect();
        SecurityContextHolder.clearContext();
    }

    /**
     * 用真实注解方法构造 mock 的 ProceedingJoinPoint
     */
    private ProceedingJoinPoint buildJoinPoint(String methodName) throws NoSuchMethodException {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        Method method = AnnotatedTarget.class.getMethod(methodName);

        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.toShortString()).thenReturn("AnnotatedTarget." + methodName + "()");
        when(pjp.getArgs()).thenReturn(new Object[0]);
        return pjp;
    }

    @Test
    @DisplayName("未超过限制时正常执行 joinPoint.proceed()")
    void belowLimit_proceeds() throws Throwable {
        ProceedingJoinPoint pjp = buildJoinPoint("rateLimitedMethod"); // maxRequests=5
        when(pjp.proceed()).thenReturn("result");

        Object result = aspect.handleRateLimit(pjp);

        assertEquals("result", result);
        verify(pjp).proceed();
    }

    @Test
    @DisplayName("超过 maxRequests 后抛出 BusinessException(429)")
    void exceedsLimit_throwsQuotaExceeded() throws Throwable {
        ProceedingJoinPoint pjp = buildJoinPoint("strictRateLimitedMethod"); // maxRequests=2
        when(pjp.proceed()).thenReturn("ok");

        // 前两次正常
        aspect.handleRateLimit(pjp);
        aspect.handleRateLimit(pjp);

        // 第三次超限
        BusinessException ex = assertThrows(BusinessException.class, () -> aspect.handleRateLimit(pjp));
        assertEquals(429, ex.getCode());
        assertTrue(ex.getMessage().contains("请求过于频繁"));
    }

    @Test
    @DisplayName("不同用户各自独立计数")
    void differentUsers_independentCounting() throws Throwable {
        // 模拟用户 1 — 用 strictRateLimitedMethod (maxRequests=2)
        Authentication auth1 = mock(Authentication.class);
        when(auth1.getDetails()).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(auth1);

        ProceedingJoinPoint pjp1 = buildJoinPoint("strictRateLimitedMethod");
        when(pjp1.proceed()).thenReturn("ok");

        aspect.handleRateLimit(pjp1);
        aspect.handleRateLimit(pjp1);

        // 用户 1 超限
        assertThrows(BusinessException.class, () -> aspect.handleRateLimit(pjp1));

        // 模拟用户 2 — 不受用户 1 计数影响
        Authentication auth2 = mock(Authentication.class);
        when(auth2.getDetails()).thenReturn(2L);
        SecurityContextHolder.getContext().setAuthentication(auth2);

        ProceedingJoinPoint pjp2 = buildJoinPoint("strictRateLimitedMethod");
        when(pjp2.proceed()).thenReturn("ok");

        // 用户 2 的计数从 0 开始，第一次调用应成功
        Object result = aspect.handleRateLimit(pjp2);
        assertEquals("ok", result);
    }

    @Test
    @DisplayName("无认证信息时使用 anonymous 作为 userId")
    void noAuth_usesAnonymous() throws Throwable {
        SecurityContextHolder.clearContext();
        ProceedingJoinPoint pjp = buildJoinPoint("rateLimitedMethod");
        when(pjp.proceed()).thenReturn("ok");

        Object result = aspect.handleRateLimit(pjp);
        assertEquals("ok", result);
    }

    // --- 辅助：带注解的目标类 ---
    public static class AnnotatedTarget {
        @RateLimit(maxRequests = 5, windowSeconds = 60)
        public String rateLimitedMethod() {
            return "ok";
        }

        @RateLimit(maxRequests = 2, windowSeconds = 60)
        public String strictRateLimitedMethod() {
            return "ok";
        }
    }
}

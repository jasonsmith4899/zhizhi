package com.zhizhi.ai.common;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RequestLogAspect 请求日志切面测试")
class RequestLogAspectTest {

    private RequestLogAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RequestLogAspect();
        ReflectionTestUtils.setField(aspect, "slowRequestMs", 2000L);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void bindRequest(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setServletPath(uri);
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    @DisplayName("正常请求返回原值且不抛异常")
    void normalRequestReturnsValue() throws Throwable {
        bindRequest("GET", "/api/v1/knowledge-bases");
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getArgs()).thenReturn(new Object[]{});
        when(pjp.proceed()).thenReturn("ok");

        Object result = aspect.around(pjp);

        assertEquals("ok", result);
        verify(pjp).proceed();
    }

    @Test
    @DisplayName("异常被记录后重新抛出")
    void exceptionIsRethrown() throws Throwable {
        bindRequest("POST", "/api/v1/knowledge-bases");
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getArgs()).thenReturn(new Object[]{"arg1"});
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> aspect.around(pjp));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    @DisplayName("对话端点 POST /api/v1/chat 跳过记录但仍执行")
    void chatEndpointSkipped() throws Throwable {
        bindRequest("POST", "/api/v1/chat");
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenReturn("chat-result");

        Object result = aspect.around(pjp);

        assertEquals("chat-result", result);
        verify(pjp, never()).getArgs();
    }

    @Test
    @DisplayName("流式对话端点 POST /api/v1/chat/stream 跳过记录")
    void chatStreamEndpointSkipped() throws Throwable {
        bindRequest("POST", "/api/v1/chat/stream");
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenReturn("stream");

        Object result = aspect.around(pjp);

        assertEquals("stream", result);
        verify(pjp, never()).getArgs();
    }

    @Test
    @DisplayName("无请求上下文时不抛异常，正常执行")
    void noRequestContextStillProceeds() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getArgs()).thenReturn(new Object[]{});
        when(pjp.proceed()).thenReturn("ok");

        Object result = aspect.around(pjp);

        assertEquals("ok", result);
    }
}

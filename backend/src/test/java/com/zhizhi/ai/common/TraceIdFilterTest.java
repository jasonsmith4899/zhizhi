package com.zhizhi.ai.common;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("TraceIdFilter 链路追踪过滤器测试")
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    @DisplayName("无 X-Trace-Id 头时生成 traceId 并写入响应头")
    void generatesTraceIdWhenAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        String traceId = response.getHeader("X-Trace-Id");
        assertNotNull(traceId);
        assertEquals(8, traceId.length());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("透传请求头中的 X-Trace-Id")
    void propagatesIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "abc12345");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals("abc12345", response.getHeader("X-Trace-Id"));
    }

    @Test
    @DisplayName("请求结束后清理 MDC")
    void clearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(MDC.get("traceId"));
    }

    @Test
    @DisplayName("MDC 在链执行期间持有 traceId")
    void mdcHoldsTraceIdDuringChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String[] captured = new String[1];
        FilterChain chain = (req, res) -> captured[0] = MDC.get("traceId");

        filter.doFilter(request, response, chain);

        assertNotNull(captured[0]);
        assertEquals(8, captured[0].length());
    }
}

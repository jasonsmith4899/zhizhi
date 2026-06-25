package com.zhizhi.ai.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BusinessException 单元测试")
class BusinessExceptionTest {

    @Test
    @DisplayName("单参数构造器默认 code=400")
    void constructor_singleArg_defaultCode400() {
        BusinessException ex = new BusinessException("出错了");
        assertEquals(400, ex.getCode());
        assertEquals("出错了", ex.getMessage());
    }

    @Test
    @DisplayName("双参数构造器指定 code 和 message")
    void constructor_twoArgs() {
        BusinessException ex = new BusinessException(429, "请求过多");
        assertEquals(429, ex.getCode());
        assertEquals("请求过多", ex.getMessage());
    }

    @Test
    @DisplayName("notFound 工厂方法返回 404，消息追加\"不存在\"")
    void notFound_appendsMessage() {
        BusinessException ex = BusinessException.notFound("文档");
        assertEquals(404, ex.getCode());
        assertEquals("文档不存在", ex.getMessage());
    }

    @Test
    @DisplayName("unauthorized() 无参返回默认未登录消息")
    void unauthorized_noArg() {
        BusinessException ex = BusinessException.unauthorized();
        assertEquals(401, ex.getCode());
        assertEquals("未登录或Token已过期", ex.getMessage());
    }

    @Test
    @DisplayName("unauthorized(message) 自定义消息")
    void unauthorized_customMessage() {
        BusinessException ex = BusinessException.unauthorized("Token无效");
        assertEquals(401, ex.getCode());
        assertEquals("Token无效", ex.getMessage());
    }

    @Test
    @DisplayName("forbidden 返回 403")
    void forbidden() {
        BusinessException ex = BusinessException.forbidden("无权限");
        assertEquals(403, ex.getCode());
        assertEquals("无权限", ex.getMessage());
    }

    @Test
    @DisplayName("badRequest 返回 400")
    void badRequest() {
        BusinessException ex = BusinessException.badRequest("参数错误");
        assertEquals(400, ex.getCode());
        assertEquals("参数错误", ex.getMessage());
    }

    @Test
    @DisplayName("quotaExceeded 返回 429")
    void quotaExceeded() {
        BusinessException ex = BusinessException.quotaExceeded("配额用尽");
        assertEquals(429, ex.getCode());
        assertEquals("配额用尽", ex.getMessage());
    }

    @Test
    @DisplayName("是 RuntimeException 的子类")
    void isRuntimeException() {
        BusinessException ex = new BusinessException("test");
        assertInstanceOf(RuntimeException.class, ex);
    }
}

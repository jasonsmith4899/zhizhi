package com.zhizhi.ai.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Result 统一响应封装测试")
class ResultTest {

    @Test
    @DisplayName("ok(data) 返回 code=200, message=success, 携带 data")
    void ok_withData() {
        Result<String> result = Result.ok("hello");
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("hello", result.getData());
        assertTrue(result.getTimestamp() > 0);
    }

    @Test
    @DisplayName("ok() 无参返回 code=200, data=null")
    void ok_noArgs() {
        Result<Void> result = Result.ok();
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("ok(message, data) 自定义 message")
    void ok_customMessage() {
        Result<Integer> result = Result.ok("操作成功", 42);
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertEquals(42, result.getData());
    }

    @Test
    @DisplayName("error(code, message) 指定错误码")
    void error_withCode() {
        Result<Void> result = Result.error(404, "未找到");
        assertEquals(404, result.getCode());
        assertEquals("未找到", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("error(message) 默认 code=500")
    void error_defaultCode() {
        Result<Void> result = Result.error("系统错误");
        assertEquals(500, result.getCode());
        assertEquals("系统错误", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("fail(message) 返回 code=400")
    void fail() {
        Result<Void> result = Result.fail("参数校验失败");
        assertEquals(400, result.getCode());
        assertEquals("参数校验失败", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("timestamp 在 ok/fail/error 调用时被设置")
    void timestamp_isSet() {
        long before = System.currentTimeMillis();
        Result<String> result = Result.ok("data");
        long after = System.currentTimeMillis();
        assertTrue(result.getTimestamp() >= before && result.getTimestamp() <= after);
    }

    @Test
    @DisplayName("全参构造器和 getter/setter 正常工作")
    void allArgsConstructor() {
        Result<String> result = new Result<>(200, "ok", "data", 12345L);
        assertEquals(200, result.getCode());
        assertEquals("ok", result.getMessage());
        assertEquals("data", result.getData());
        assertEquals(12345L, result.getTimestamp());
    }

    @Test
    @DisplayName("data 泛型支持不同类型")
    void genericTypes() {
        Result<Integer> intResult = Result.ok(100);
        assertEquals(100, intResult.getData());

        Result<Boolean> boolResult = Result.ok(true);
        assertTrue(boolResult.getData());
    }
}

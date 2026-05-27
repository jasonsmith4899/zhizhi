package com.zhizhi.ai.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BusinessException 单元测试")
class BusinessExceptionTest {

    @Test
    @DisplayName("默认构造 - code=400")
    void defaultConstructor() {
        BusinessException e = new BusinessException("出错了");
        assertThat(e.getCode()).isEqualTo(400);
        assertThat(e.getMessage()).isEqualTo("出错了");
    }

    @Test
    @DisplayName("指定code构造")
    void codeConstructor() {
        BusinessException e = new BusinessException(500, "服务器错误");
        assertThat(e.getCode()).isEqualTo(500);
        assertThat(e.getMessage()).isEqualTo("服务器错误");
    }

    @Test
    @DisplayName("notFound - 404")
    void notFound() {
        BusinessException e = BusinessException.notFound("知识库");
        assertThat(e.getCode()).isEqualTo(404);
        assertThat(e.getMessage()).contains("不存在");
    }

    @Test
    @DisplayName("unauthorized - 401")
    void unauthorized() {
        BusinessException e = BusinessException.unauthorized();
        assertThat(e.getCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("unauthorized(消息) - 401")
    void unauthorizedWithMessage() {
        BusinessException e = BusinessException.unauthorized("Token过期");
        assertThat(e.getCode()).isEqualTo(401);
        assertThat(e.getMessage()).isEqualTo("Token过期");
    }

    @Test
    @DisplayName("forbidden - 403")
    void forbidden() {
        BusinessException e = BusinessException.forbidden("无权限");
        assertThat(e.getCode()).isEqualTo(403);
        assertThat(e.getMessage()).isEqualTo("无权限");
    }

    @Test
    @DisplayName("badRequest - 400")
    void badRequest() {
        BusinessException e = BusinessException.badRequest("参数错误");
        assertThat(e.getCode()).isEqualTo(400);
        assertThat(e.getMessage()).isEqualTo("参数错误");
    }

    @Test
    @DisplayName("quotaExceeded - 429")
    void quotaExceeded() {
        BusinessException e = BusinessException.quotaExceeded("超出配额");
        assertThat(e.getCode()).isEqualTo(429);
    }
}

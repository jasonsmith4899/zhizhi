package com.zhizhi.ai.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 全局异常处理测试")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    // ---------- handleBusiness ----------

    @Test
    @DisplayName("BusinessException 返回对应状态码和消息")
    void handleBusiness() {
        BusinessException ex = BusinessException.notFound("文档");
        ResponseEntity<Result<Void>> response = handler.handleBusiness(ex);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getCode());
        assertEquals("文档不存在", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    @DisplayName("BusinessException(401) 返回 401")
    void handleBusiness_unauthorized() {
        BusinessException ex = BusinessException.unauthorized();
        ResponseEntity<Result<Void>> response = handler.handleBusiness(ex);

        assertEquals(401, response.getStatusCode().value());
        assertEquals(401, response.getBody().getCode());
    }

    @Test
    @DisplayName("BusinessException(429) 返回 429")
    void handleBusiness_quotaExceeded() {
        BusinessException ex = BusinessException.quotaExceeded("配额超限");
        ResponseEntity<Result<Void>> response = handler.handleBusiness(ex);

        assertEquals(429, response.getStatusCode().value());
        assertEquals("配额超限", response.getBody().getMessage());
    }

    // ---------- handleValidation ----------

    @Test
    @DisplayName("MethodArgumentNotValidException 拼接字段错误消息返回 400")
    void handleValidation() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error1 = new FieldError("obj", "name", "名称不能为空");
        FieldError error2 = new FieldError("obj", "email", "邮箱格式错误");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                (MethodParameter) null, bindingResult);

        ResponseEntity<Result<Void>> response = handler.handleValidation(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("名称不能为空"));
        assertTrue(response.getBody().getMessage().contains("邮箱格式错误"));
    }

    @Test
    @DisplayName("单个字段错误时消息不带逗号")
    void handleValidation_singleError() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error = new FieldError("obj", "name", "名称不能为空");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                (MethodParameter) null, bindingResult);

        ResponseEntity<Result<Void>> response = handler.handleValidation(ex);
        assertEquals("名称不能为空", response.getBody().getMessage());
    }

    // ---------- handleBadCredentials ----------

    @Test
    @DisplayName("BadCredentialsException 返回 401")
    void handleBadCredentials() {
        BadCredentialsException ex = new BadCredentialsException("bad");
        ResponseEntity<Result<Void>> response = handler.handleBadCredentials(ex);

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getCode());
        assertEquals("用户名或密码错误", response.getBody().getMessage());
    }

    // ---------- handleMaxUpload ----------

    @Test
    @DisplayName("MaxUploadSizeExceededException 返回 400 文件大小超限消息")
    void handleMaxUpload() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(20 * 1024 * 1024);
        ResponseEntity<Result<Void>> response = handler.handleMaxUpload(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("文件大小超过限制"));
    }

    // ---------- handleGeneral ----------

    @Test
    @DisplayName("未知异常返回 500 系统内部错误")
    void handleGeneral() {
        Exception ex = new RuntimeException("unexpected");
        ResponseEntity<Result<Void>> response = handler.handleGeneral(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getCode());
        assertEquals("系统内部错误，请稍后重试", response.getBody().getMessage());
    }

    @Test
    @DisplayName("NullPointerException 也被兜底处理为 500")
    void handleGeneral_npe() {
        NullPointerException ex = new NullPointerException();
        ResponseEntity<Result<Void>> response = handler.handleGeneral(ex);

        assertEquals(500, response.getStatusCode().value());
    }

    // ---------- 日志增强后行为不变验证 ----------

    @Test
    @DisplayName("校验异常记录日志但返回体不变")
    void handleValidationStillReturnsBadRequest() throws Exception {
        org.springframework.core.MethodParameter mp = mock(org.springframework.core.MethodParameter.class);
        org.springframework.validation.BindingResult br = mock(org.springframework.validation.BindingResult.class);
        when(br.getFieldErrors()).thenReturn(java.util.List.of(
                new FieldError("obj", "username", "不能为空")));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mp, br);

        ResponseEntity<Result<Void>> response = handler.handleValidation(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("不能为空", response.getBody().getMessage());
    }

    @Test
    @DisplayName("通用异常返回 500 且不泄露内部细节")
    void handleGeneralReturns500() {
        ResponseEntity<Result<Void>> response =
                handler.handleGeneral(new RuntimeException("internal db error xyz"));

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("系统内部错误，请稍后重试", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("xyz"));
    }
}

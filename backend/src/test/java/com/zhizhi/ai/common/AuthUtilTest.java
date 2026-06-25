package com.zhizhi.ai.common;

import com.zhizhi.ai.config.SecurityConfig.ApiKeyAuthDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthUtil 单元测试")
class AuthUtilTest {

    @InjectMocks
    private AuthUtil authUtil;

    // ---------- getUserId ----------

    @Test
    @DisplayName("JWT 认证：details 为 Long 时返回 userId")
    void getUserId_jwtAuth_returnsUserId() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(42L);

        assertEquals(42L, authUtil.getUserId(auth));
    }

    @Test
    @DisplayName("API Key 认证：details 为 ApiKeyAuthDetail 时返回 userId")
    void getUserId_apiKeyAuth_returnsUserId() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(new ApiKeyAuthDetail(42L, 100L));

        assertEquals(42L, authUtil.getUserId(auth));
    }

    @Test
    @DisplayName("details 为未知类型时抛出 BusinessException(401)")
    void getUserId_unknownType_throws() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn("unknown");

        BusinessException ex = assertThrows(BusinessException.class, () -> authUtil.getUserId(auth));
        assertEquals(401, ex.getCode());
        assertTrue(ex.getMessage().contains("无法获取用户信息"));
    }

    @Test
    @DisplayName("details 为 null 时抛出 BusinessException(401)")
    void getUserId_nullDetails_throws() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> authUtil.getUserId(auth));
        assertEquals(401, ex.getCode());
    }

    // ---------- isApiKeyAuth ----------

    @Test
    @DisplayName("API Key 认证时 isApiKeyAuth 返回 true")
    void isApiKeyAuth_true() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(new ApiKeyAuthDetail(1L, 10L));

        assertTrue(authUtil.isApiKeyAuth(auth));
    }

    @Test
    @DisplayName("JWT 认证时 isApiKeyAuth 返回 false")
    void isApiKeyAuth_false() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(1L);

        assertFalse(authUtil.isApiKeyAuth(auth));
    }

    // ---------- getApiKeyId ----------

    @Test
    @DisplayName("API Key 认证时返回 apiKeyId")
    void getApiKeyId_returnsId() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(new ApiKeyAuthDetail(1L, 100L));

        assertEquals(100L, authUtil.getApiKeyId(auth));
    }

    @Test
    @DisplayName("JWT 认证时 getApiKeyId 返回 null")
    void getApiKeyId_jwtAuth_returnsNull() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(1L);

        assertNull(authUtil.getApiKeyId(auth));
    }
}

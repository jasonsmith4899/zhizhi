package com.zhizhi.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig 单元测试")
class SecurityConfigTest {

    @Mock
    private SecurityConfig.JwtAuthFilter jwtAuthFilter;

    @Test
    @DisplayName("passwordEncoder 返回 BCryptPasswordEncoder")
    void passwordEncoder_returnsBCrypt() {
        SecurityConfig config = new SecurityConfig(jwtAuthFilter);
        PasswordEncoder encoder = config.passwordEncoder();

        assertNotNull(encoder);
        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
    }

    @Test
    @DisplayName("passwordEncoder 能正确编码和验证密码")
    void passwordEncoder_encodesAndMatches() {
        SecurityConfig config = new SecurityConfig(jwtAuthFilter);
        PasswordEncoder encoder = config.passwordEncoder();

        String raw = "mypassword";
        String encoded = encoder.encode(raw);

        assertTrue(encoder.matches(raw, encoded));
        assertFalse(encoder.matches("wrong", encoded));
    }

    @Test
    @DisplayName("corsConfigurationSource 返回非 null")
    void corsConfigurationSource_notNull() {
        SecurityConfig config = new SecurityConfig(jwtAuthFilter);

        assertNotNull(config.corsConfigurationSource());
    }

    @Test
    @DisplayName("ApiKeyAuthDetail record 正确存储 userId 和 apiKeyId")
    void apiKeyAuthDetailRecord() {
        SecurityConfig.ApiKeyAuthDetail detail = new SecurityConfig.ApiKeyAuthDetail(10L, 20L);

        assertEquals(10L, detail.userId());
        assertEquals(20L, detail.apiKeyId());
    }

    @Test
    @DisplayName("ApiKeyAuthDetail record equals/hashCode")
    void apiKeyAuthDetailEqualsHashCode() {
        SecurityConfig.ApiKeyAuthDetail d1 = new SecurityConfig.ApiKeyAuthDetail(1L, 2L);
        SecurityConfig.ApiKeyAuthDetail d2 = new SecurityConfig.ApiKeyAuthDetail(1L, 2L);
        SecurityConfig.ApiKeyAuthDetail d3 = new SecurityConfig.ApiKeyAuthDetail(1L, 3L);

        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
        assertNotEquals(d1, d3);
    }
}

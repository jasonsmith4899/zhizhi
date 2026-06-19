package com.zhizhi.ai.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil 单元测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String SECRET = "zhizhi-ai-test-secret-key-must-be-long-enough-32chars!!";
    private final long EXPIRATION = 86400000; // 24h

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION);
    }

    @Test
    @DisplayName("生成Token并解析用户名")
    void generateToken_andGetUsername() {
        String token = jwtUtil.generateToken(1L, "testuser");

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.getUsername(token)).isEqualTo("testuser");
    }

    @Test
    @DisplayName("解析UserId")
    void getUserId() {
        String token = jwtUtil.generateToken(42L, "admin");

        assertThat(jwtUtil.getUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("有效Token验证通过")
    void validateToken_valid() {
        String token = jwtUtil.generateToken(1L, "testuser");

        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("无效Token验证失败")
    void validateToken_invalid() {
        assertThat(jwtUtil.validateToken("invalid.token.value")).isFalse();
    }

    @Test
    @DisplayName("空Token验证失败")
    void validateToken_empty() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("篡改Token验证失败")
    void validateToken_tampered() {
        String token = jwtUtil.generateToken(1L, "testuser");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("不同密钥生成的Token不互通")
    void differentSecret_rejectsToken() {
        String token = jwtUtil.generateToken(1L, "testuser");
        JwtUtil otherUtil = new JwtUtil("different-secret-key-must-be-long-enough-32chars!!", EXPIRATION);

        assertThat(otherUtil.validateToken(token)).isFalse();
    }
}

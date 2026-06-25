package com.zhizhi.ai.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil 单元测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET = "test-jwt-secret-key-for-unit-testing-purposes-only!!";
    private static final long EXPIRATION = 86400000L;         // 24h
    private static final long REFRESH_EXPIRATION = 604800000L; // 7d

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION, REFRESH_EXPIRATION);
    }

    // ---------- generateToken ----------

    @Test
    @DisplayName("generateToken(userId, username) 生成可解析的 access token")
    void generateToken_twoArgs() {
        String token = jwtUtil.generateToken(1L, "alice");
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(1L, jwtUtil.getUserId(token));
        assertEquals("alice", jwtUtil.getUsername(token));
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("generateToken(userId, username, tenantId) 包含 tenantId claim")
    void generateToken_threeArgs() {
        String token = jwtUtil.generateToken(1L, "alice", 10L);
        assertNotNull(token);
        assertEquals(1L, jwtUtil.getUserId(token));
        assertEquals("alice", jwtUtil.getUsername(token));
        assertTrue(jwtUtil.validateToken(token));
    }

    // ---------- generateRefreshToken ----------

    @Test
    @DisplayName("generateRefreshToken(userId) 生成 refresh token")
    void generateRefreshToken_noVersion() {
        String token = jwtUtil.generateRefreshToken(1L);
        assertNotNull(token);
        assertEquals(1L, jwtUtil.getUserId(token));
        assertTrue(jwtUtil.validateRefreshToken(token));
    }

    @Test
    @DisplayName("generateRefreshToken(userId, version) 包含 ver claim")
    void generateRefreshToken_withVersion() {
        String token = jwtUtil.generateRefreshToken(1L, 3);
        assertNotNull(token);
        assertEquals(1L, jwtUtil.getUserId(token));
        assertEquals(3, jwtUtil.getRefreshTokenVersion(token));
        assertTrue(jwtUtil.validateRefreshToken(token));
    }

    // ---------- validateToken ----------

    @Test
    @DisplayName("有效 token 返回 true")
    void validateToken_valid() {
        String token = jwtUtil.generateToken(1L, "alice");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("篡改 token 返回 false")
    void validateToken_tampered() {
        String token = jwtUtil.generateToken(1L, "alice");
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    @DisplayName("空字符串 token 返回 false")
    void validateToken_empty() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    @DisplayName("随机字符串 token 返回 false")
    void validateToken_randomString() {
        assertFalse(jwtUtil.validateToken("not.a.jwt"));
    }

    // ---------- validateRefreshToken ----------

    @Test
    @DisplayName("access token 不是 refresh token")
    void validateRefreshToken_accessTokenReturnsFalse() {
        String accessToken = jwtUtil.generateToken(1L, "alice");
        assertFalse(jwtUtil.validateRefreshToken(accessToken));
    }

    @Test
    @DisplayName("refresh token 验证通过")
    void validateRefreshToken_valid() {
        String refreshToken = jwtUtil.generateRefreshToken(1L);
        assertTrue(jwtUtil.validateRefreshToken(refreshToken));
    }

    // ---------- getUserId / getUsername ----------

    @Test
    @DisplayName("getUserId 正确提取用户 ID")
    void getUserId() {
        String token = jwtUtil.generateToken(42L, "bob");
        assertEquals(42L, jwtUtil.getUserId(token));
    }

    @Test
    @DisplayName("getUsername 正确提取用户名")
    void getUsername() {
        String token = jwtUtil.generateToken(42L, "bob");
        assertEquals("bob", jwtUtil.getUsername(token));
    }

    // ---------- getRefreshTokenVersion ----------

    @Test
    @DisplayName("无 ver claim 时返回 null")
    void getRefreshTokenVersion_noVersion() {
        String token = jwtUtil.generateRefreshToken(1L);
        assertNull(jwtUtil.getRefreshTokenVersion(token));
    }

    @Test
    @DisplayName("有 ver claim 时返回版本号")
    void getRefreshTokenVersion_withVersion() {
        String token = jwtUtil.generateRefreshToken(1L, 5);
        assertEquals(5, jwtUtil.getRefreshTokenVersion(token));
    }

    // ---------- 不同密钥的 token 无法验证 ----------

    @Test
    @DisplayName("不同密钥签名的 token 验证失败")
    void validateToken_wrongSecret() {
        JwtUtil otherJwtUtil = new JwtUtil("other-secret-key-must-be-long-enough-for-hmac-sha!!", EXPIRATION, REFRESH_EXPIRATION);
        String token = otherJwtUtil.generateToken(1L, "alice");
        assertFalse(jwtUtil.validateToken(token));
    }
}

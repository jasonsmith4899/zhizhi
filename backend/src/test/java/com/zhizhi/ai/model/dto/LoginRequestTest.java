package com.zhizhi.ai.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LoginRequest 单元测试")
class LoginRequestTest {

    @Test
    @DisplayName("Getter/Setter")
    void getterSetter() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("123456");

        assertEquals("admin", req.getUsername());
        assertEquals("123456", req.getPassword());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        LoginRequest r1 = new LoginRequest();
        r1.setUsername("u1");
        r1.setPassword("p1");

        LoginRequest r2 = new LoginRequest();
        r2.setUsername("u1");
        r2.setPassword("p1");

        assertEquals(r1, r2);
    }
}

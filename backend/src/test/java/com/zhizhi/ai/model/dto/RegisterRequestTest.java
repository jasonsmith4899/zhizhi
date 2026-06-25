package com.zhizhi.ai.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RegisterRequest 单元测试")
class RegisterRequestTest {

    @Test
    @DisplayName("Getter/Setter")
    void getterSetter() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setEmail("new@example.com");
        req.setPassword("password123");

        assertEquals("newuser", req.getUsername());
        assertEquals("new@example.com", req.getEmail());
        assertEquals("password123", req.getPassword());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        RegisterRequest r1 = new RegisterRequest();
        r1.setUsername("u1");
        r1.setEmail("u1@e.com");
        r1.setPassword("p1");

        RegisterRequest r2 = new RegisterRequest();
        r2.setUsername("u1");
        r2.setEmail("u1@e.com");
        r2.setPassword("p1");

        assertEquals(r1, r2);
    }
}

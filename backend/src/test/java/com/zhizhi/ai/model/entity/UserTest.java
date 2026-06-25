package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User 实体单元测试")
class UserTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedPwd");
        user.setPlan("basic");
        user.setRefreshTokenVersion(2);
        user.setDailyQueriesUsed(10);
        LocalDateTime now = LocalDateTime.now();
        user.setDailyQueriesResetAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setNewUser(true);

        assertEquals(1L, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("hashedPwd", user.getPassword());
        assertEquals("basic", user.getPlan());
        assertEquals(2, user.getRefreshTokenVersion());
        assertEquals(10, user.getDailyQueriesUsed());
        assertEquals(now, user.getDailyQueriesResetAt());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
        assertTrue(user.isNewUser());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User(1L, "user1", "u1@e.com", "pwd",
                "free", 0, 5, now, now, now, false);

        assertEquals(1L, user.getId());
        assertEquals("user1", user.getUsername());
    }

    @Test
    @DisplayName("Builder 验证默认值")
    void builderDefaults() {
        User user = User.builder().build();
        assertEquals("free", user.getPlan());
        assertEquals(0, user.getRefreshTokenVersion());
        assertEquals(0, user.getDailyQueriesUsed());
        assertFalse(user.isNewUser());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt, updatedAt, dailyQueriesResetAt")
    void prePersist() {
        User user = User.builder().build();
        user.onCreate();

        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertNotNull(user.getDailyQueriesResetAt());
        // dailyQueriesResetAt 应该是明天的 00:00:00
        LocalDateTime reset = user.getDailyQueriesResetAt();
        assertEquals(0, reset.getHour());
        assertEquals(0, reset.getMinute());
        assertEquals(0, reset.getSecond());
    }

    @Test
    @DisplayName("@PreUpdate 更新 updatedAt")
    void preUpdate() {
        User user = User.builder().build();
        user.onCreate();
        LocalDateTime original = user.getCreatedAt();

        user.onUpdate();
        assertNotNull(user.getUpdatedAt());
        assertEquals(original, user.getCreatedAt());
    }

    @Test
    @DisplayName("newUser 是 @Transient 字段")
    void transientNewUser() {
        User user = User.builder().newUser(true).build();
        assertTrue(user.isNewUser());

        user.setNewUser(false);
        assertFalse(user.isNewUser());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        User u1 = User.builder().id(1L).username("u1").build();
        User u2 = User.builder().id(1L).username("u1").build();
        User u3 = User.builder().id(2L).username("u2").build();

        assertEquals(u1, u2);
        assertNotEquals(u1, u3);
    }
}

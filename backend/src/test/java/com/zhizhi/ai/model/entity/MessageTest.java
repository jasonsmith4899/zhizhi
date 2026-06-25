package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Message 实体单元测试")
class MessageTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        Message msg = new Message();
        msg.setId(1L);
        msg.setConversationId(10L);
        msg.setTenantId(20L);
        msg.setRole("user");
        msg.setContent("你好");
        msg.setSourceDocuments("[{\"docId\":1}]");
        msg.setTokenCount(100);
        LocalDateTime now = LocalDateTime.now();
        msg.setCreatedAt(now);

        assertEquals(1L, msg.getId());
        assertEquals(10L, msg.getConversationId());
        assertEquals(20L, msg.getTenantId());
        assertEquals("user", msg.getRole());
        assertEquals("你好", msg.getContent());
        assertEquals("[{\"docId\":1}]", msg.getSourceDocuments());
        assertEquals(100, msg.getTokenCount());
        assertEquals(now, msg.getCreatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Message msg = new Message(1L, 10L, 20L, "assistant", "回复",
                "[{}]", 50, now);

        assertEquals(1L, msg.getId());
        assertEquals("assistant", msg.getRole());
        assertEquals("回复", msg.getContent());
    }

    @Test
    @DisplayName("Builder")
    void builder() {
        Message msg = Message.builder()
                .id(1L).conversationId(10L).tenantId(20L)
                .role("system").content("系统消息").build();

        assertEquals(1L, msg.getId());
        assertEquals("system", msg.getRole());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt")
    void prePersist() {
        Message msg = Message.builder().build();
        msg.onCreate();
        assertNotNull(msg.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        Message m1 = Message.builder().id(1L).role("user").content("hi").build();
        Message m2 = Message.builder().id(1L).role("user").content("hi").build();
        Message m3 = Message.builder().id(2L).role("assistant").content("hello").build();

        assertEquals(m1, m2);
        assertNotEquals(m1, m3);
    }
}

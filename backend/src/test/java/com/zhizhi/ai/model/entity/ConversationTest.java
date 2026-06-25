package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Conversation 实体单元测试")
class ConversationTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        Conversation conv = new Conversation();
        conv.setId(1L);
        conv.setSessionId("sess-123");
        conv.setUserId(10L);
        conv.setKnowledgeBaseId(20L);
        conv.setTenantId(30L);
        conv.setChannel("miniprogram");
        conv.setTitle("新对话");
        conv.setMessageCount(5);
        LocalDateTime now = LocalDateTime.now();
        conv.setCreatedAt(now);
        conv.setUpdatedAt(now);

        assertEquals(1L, conv.getId());
        assertEquals("sess-123", conv.getSessionId());
        assertEquals(10L, conv.getUserId());
        assertEquals(20L, conv.getKnowledgeBaseId());
        assertEquals(30L, conv.getTenantId());
        assertEquals("miniprogram", conv.getChannel());
        assertEquals("新对话", conv.getTitle());
        assertEquals(5, conv.getMessageCount());
        assertEquals(now, conv.getCreatedAt());
        assertEquals(now, conv.getUpdatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Conversation conv = new Conversation(1L, "sess-1", 10L, 20L, 30L,
                "api", "title", 3, now, now);

        assertEquals(1L, conv.getId());
        assertEquals("sess-1", conv.getSessionId());
        assertEquals("api", conv.getChannel());
    }

    @Test
    @DisplayName("Builder 验证默认值")
    void builderDefaults() {
        Conversation conv = Conversation.builder().build();
        assertEquals("web", conv.getChannel());
        assertEquals(0, conv.getMessageCount());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt 和 updatedAt")
    void prePersist() {
        Conversation conv = Conversation.builder().build();
        conv.onCreate();
        assertNotNull(conv.getCreatedAt());
        assertNotNull(conv.getUpdatedAt());
    }

    @Test
    @DisplayName("@PreUpdate 更新 updatedAt")
    void preUpdate() {
        Conversation conv = Conversation.builder().build();
        conv.onCreate();
        LocalDateTime original = conv.getCreatedAt();

        conv.onUpdate();
        assertNotNull(conv.getUpdatedAt());
        assertEquals(original, conv.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        Conversation conv1 = Conversation.builder().id(1L).sessionId("s1").build();
        Conversation conv2 = Conversation.builder().id(1L).sessionId("s1").build();
        Conversation conv3 = Conversation.builder().id(2L).sessionId("s2").build();

        assertEquals(conv1, conv2);
        assertNotEquals(conv1, conv3);
    }
}

package com.zhizhi.ai.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChatRequest 单元测试")
class ChatRequestTest {

    @Test
    @DisplayName("Getter/Setter")
    void getterSetter() {
        ChatRequest req = new ChatRequest();
        req.setMessage("你好");
        req.setApiKeyId(1L);
        req.setKnowledgeBaseId(2L);
        req.setSessionId("sess-123");
        req.setTagIds(List.of(10L, 20L));

        assertEquals("你好", req.getMessage());
        assertEquals(1L, req.getApiKeyId());
        assertEquals(2L, req.getKnowledgeBaseId());
        assertEquals("sess-123", req.getSessionId());
        assertEquals(2, req.getTagIds().size());
    }

    @Test
    @DisplayName("tagIds 可以为 null")
    void tagIdsNull() {
        ChatRequest req = new ChatRequest();
        req.setMessage("test");
        assertNull(req.getTagIds());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        ChatRequest r1 = new ChatRequest();
        r1.setMessage("msg");
        r1.setApiKeyId(1L);

        ChatRequest r2 = new ChatRequest();
        r2.setMessage("msg");
        r2.setApiKeyId(1L);

        assertEquals(r1, r2);
    }
}

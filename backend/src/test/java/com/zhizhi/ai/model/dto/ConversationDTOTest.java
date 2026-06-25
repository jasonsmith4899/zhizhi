package com.zhizhi.ai.model.dto;

import com.zhizhi.ai.model.entity.Conversation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConversationDTO 单元测试")
class ConversationDTOTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(1L);
        dto.setTitle("对话标题");
        dto.setSessionId("sess-1");
        dto.setChannel("web");
        dto.setMessageCount(5);
        LocalDateTime now = LocalDateTime.now();
        dto.setCreatedAt(now);
        dto.setUpdatedAt(now);

        assertEquals(1L, dto.getId());
        assertEquals("对话标题", dto.getTitle());
        assertEquals("sess-1", dto.getSessionId());
        assertEquals("web", dto.getChannel());
        assertEquals(5, dto.getMessageCount());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now, dto.getUpdatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        ConversationDTO dto = new ConversationDTO(1L, "title", "sess",
                "api", 3, now, now);

        assertEquals(1L, dto.getId());
        assertEquals("title", dto.getTitle());
    }

    @Test
    @DisplayName("Builder")
    void builder() {
        ConversationDTO dto = ConversationDTO.builder().id(1L).title("t").build();
        assertEquals(1L, dto.getId());
        assertEquals("t", dto.getTitle());
    }

    @Test
    @DisplayName("fromEntity 正确映射")
    void fromEntity() {
        Conversation conv = Conversation.builder()
                .id(1L)
                .title("标题")
                .sessionId("sess-1")
                .channel("miniprogram")
                .messageCount(10)
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 2, 0, 0))
                .build();

        ConversationDTO dto = ConversationDTO.fromEntity(conv);

        assertEquals(1L, dto.getId());
        assertEquals("标题", dto.getTitle());
        assertEquals("sess-1", dto.getSessionId());
        assertEquals("miniprogram", dto.getChannel());
        assertEquals(10, dto.getMessageCount());
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2024, 1, 2, 0, 0), dto.getUpdatedAt());
    }
}

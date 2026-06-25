package com.zhizhi.ai.model.dto;

import com.zhizhi.ai.model.entity.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MessageDTO 单元测试")
class MessageDTOTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        MessageDTO dto = new MessageDTO();
        dto.setId(1L);
        dto.setRole("user");
        dto.setContent("你好");
        LocalDateTime now = LocalDateTime.now();
        dto.setCreatedAt(now);
        dto.setSourceReferences(List.of());

        assertEquals(1L, dto.getId());
        assertEquals("user", dto.getRole());
        assertEquals("你好", dto.getContent());
        assertEquals(now, dto.getCreatedAt());
        assertNotNull(dto.getSourceReferences());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        List<ChatResponse.SourceReference> refs = List.of();
        MessageDTO dto = new MessageDTO(1L, "assistant", "回复", now, refs);

        assertEquals(1L, dto.getId());
        assertEquals("assistant", dto.getRole());
    }

    @Test
    @DisplayName("Builder")
    void builder() {
        MessageDTO dto = MessageDTO.builder().id(1L).role("user").content("hi").build();
        assertEquals(1L, dto.getId());
        assertEquals("user", dto.getRole());
    }

    @Test
    @DisplayName("fromEntity 无 sourceDocuments 返回空列表")
    void fromEntity_noSourceDocs() {
        Message msg = Message.builder()
                .id(1L)
                .role("user")
                .content("你好")
                .sourceDocuments(null)
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .build();

        MessageDTO dto = MessageDTO.fromEntity(msg);

        assertEquals(1L, dto.getId());
        assertEquals("user", dto.getRole());
        assertEquals("你好", dto.getContent());
        assertNotNull(dto.getSourceReferences());
        assertTrue(dto.getSourceReferences().isEmpty());
    }

    @Test
    @DisplayName("fromEntity 有 sourceDocuments 解析成功")
    void fromEntity_withSourceDocs() {
        String json = "[{\"documentId\":1,\"documentName\":\"doc.pdf\",\"content\":\"text\",\"score\":0.9,\"chunkIndex\":0}]";
        Message msg = Message.builder()
                .id(1L)
                .role("assistant")
                .content("回答")
                .sourceDocuments(json)
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .build();

        MessageDTO dto = MessageDTO.fromEntity(msg);

        assertEquals(1L, dto.getId());
        assertNotNull(dto.getSourceReferences());
        assertEquals(1, dto.getSourceReferences().size());
        assertEquals(1L, dto.getSourceReferences().get(0).getDocumentId());
    }

    @Test
    @DisplayName("fromEntity sourceDocuments 为空字符串返回空列表")
    void fromEntity_blankSourceDocs() {
        Message msg = Message.builder()
                .id(1L)
                .role("user")
                .content("hi")
                .sourceDocuments("   ")
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .build();

        MessageDTO dto = MessageDTO.fromEntity(msg);
        assertTrue(dto.getSourceReferences().isEmpty());
    }

    @Test
    @DisplayName("fromEntity sourceDocuments 为无效 JSON 返回空列表")
    void fromEntity_invalidJson() {
        Message msg = Message.builder()
                .id(1L)
                .role("user")
                .content("hi")
                .sourceDocuments("not json")
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .build();

        MessageDTO dto = MessageDTO.fromEntity(msg);
        assertTrue(dto.getSourceReferences().isEmpty());
    }
}

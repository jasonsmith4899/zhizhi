package com.zhizhi.ai.model.dto;

import com.zhizhi.ai.model.entity.KnowledgeBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KnowledgeBaseDTO 单元测试")
class KnowledgeBaseDTOTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        KnowledgeBaseDTO dto = new KnowledgeBaseDTO();
        dto.setId(1L);
        dto.setName("kb");
        dto.setDescription("desc");
        dto.setStatus("active");
        dto.setDocumentCount(5);
        dto.setChunkCount(50);
        LocalDateTime now = LocalDateTime.now();
        dto.setCreatedAt(now);

        assertEquals(1L, dto.getId());
        assertEquals("kb", dto.getName());
        assertEquals("desc", dto.getDescription());
        assertEquals("active", dto.getStatus());
        assertEquals(5, dto.getDocumentCount());
        assertEquals(50, dto.getChunkCount());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        KnowledgeBaseDTO dto = new KnowledgeBaseDTO(1L, "kb", "desc",
                "active", 3, 30, now);

        assertEquals(1L, dto.getId());
        assertEquals("kb", dto.getName());
    }

    @Test
    @DisplayName("Builder")
    void builder() {
        KnowledgeBaseDTO dto = KnowledgeBaseDTO.builder().id(1L).name("test").build();
        assertEquals(1L, dto.getId());
    }

    @Test
    @DisplayName("fromEntity 正确映射")
    void fromEntity() {
        KnowledgeBase kb = KnowledgeBase.builder()
                .id(1L)
                .name("知识库")
                .description("描述")
                .status("active")
                .documentCount(10)
                .chunkCount(100)
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .build();

        KnowledgeBaseDTO dto = KnowledgeBaseDTO.fromEntity(kb);

        assertEquals(1L, dto.getId());
        assertEquals("知识库", dto.getName());
        assertEquals("描述", dto.getDescription());
        assertEquals("active", dto.getStatus());
        assertEquals(10, dto.getDocumentCount());
        assertEquals(100, dto.getChunkCount());
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), dto.getCreatedAt());
    }
}

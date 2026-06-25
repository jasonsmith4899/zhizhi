package com.zhizhi.ai.model.dto;

import com.zhizhi.ai.model.entity.Document;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DocumentListDTO 单元测试")
class DocumentListDTOTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        DocumentListDTO dto = new DocumentListDTO();
        dto.setId(1L);
        dto.setFilename("test.pdf");
        dto.setFileType("pdf");
        dto.setFileSize(1024L);
        dto.setStatus("ready");
        dto.setChunkCount(5);
        dto.setCategoryId(100L);
        LocalDateTime now = LocalDateTime.now();
        dto.setCreatedAt(now);
        dto.setUpdatedAt(now);

        assertEquals(1L, dto.getId());
        assertEquals("test.pdf", dto.getFilename());
        assertEquals("pdf", dto.getFileType());
        assertEquals(1024L, dto.getFileSize());
        assertEquals("ready", dto.getStatus());
        assertEquals(5, dto.getChunkCount());
        assertEquals(100L, dto.getCategoryId());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now, dto.getUpdatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        DocumentListDTO dto = new DocumentListDTO(1L, "f.txt", "txt", 500L,
                "ready", 3, 100L, now, now);

        assertEquals(1L, dto.getId());
        assertEquals("f.txt", dto.getFilename());
    }

    @Test
    @DisplayName("Builder")
    void builder() {
        DocumentListDTO dto = DocumentListDTO.builder().id(1L).filename("f").build();
        assertEquals(1L, dto.getId());
        assertEquals("f", dto.getFilename());
    }

    @Test
    @DisplayName("fromEntity 正确映射")
    void fromEntity() {
        Document doc = Document.builder()
                .id(1L)
                .filename("test.pdf")
                .fileType("pdf")
                .fileSize(2048L)
                .status("ready")
                .chunkCount(10)
                .categoryId(50L)
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 2, 0, 0))
                .build();

        DocumentListDTO dto = DocumentListDTO.fromEntity(doc);

        assertEquals(1L, dto.getId());
        assertEquals("test.pdf", dto.getFilename());
        assertEquals("pdf", dto.getFileType());
        assertEquals(2048L, dto.getFileSize());
        assertEquals("ready", dto.getStatus());
        assertEquals(10, dto.getChunkCount());
        assertEquals(50L, dto.getCategoryId());
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2024, 1, 2, 0, 0), dto.getUpdatedAt());
    }
}

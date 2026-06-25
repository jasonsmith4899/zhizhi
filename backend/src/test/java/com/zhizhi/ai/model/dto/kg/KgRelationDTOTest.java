package com.zhizhi.ai.model.dto.kg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KgRelationDTO 单元测试")
class KgRelationDTOTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        KgRelationDTO dto = new KgRelationDTO();
        dto.setId(1L);
        dto.setSourceName("张三");
        dto.setTargetName("公司A");
        dto.setPredicate("任职于");
        dto.setConfidence(0.9f);
        dto.setDocumentId(10L);
        dto.setDocumentName("doc.pdf");
        LocalDateTime now = LocalDateTime.now();
        dto.setCreatedAt(now);

        assertEquals(1L, dto.getId());
        assertEquals("张三", dto.getSourceName());
        assertEquals("公司A", dto.getTargetName());
        assertEquals("任职于", dto.getPredicate());
        assertEquals(0.9f, dto.getConfidence());
        assertEquals(10L, dto.getDocumentId());
        assertEquals("doc.pdf", dto.getDocumentName());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        KgRelationDTO dto = new KgRelationDTO(1L, "A", "B", "关系",
                0.8f, 10L, "doc.pdf", now);

        assertEquals(1L, dto.getId());
        assertEquals("A", dto.getSourceName());
        assertEquals("B", dto.getTargetName());
        assertEquals("关系", dto.getPredicate());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        LocalDateTime now = LocalDateTime.now();
        KgRelationDTO d1 = new KgRelationDTO(1L, "A", "B", "p", 0.5f, 1L, "d", now);
        KgRelationDTO d2 = new KgRelationDTO(1L, "A", "B", "p", 0.5f, 1L, "d", now);
        KgRelationDTO d3 = new KgRelationDTO(2L, "A", "B", "p", 0.5f, 1L, "d", now);

        assertEquals(d1, d2);
        assertNotEquals(d1, d3);
    }
}

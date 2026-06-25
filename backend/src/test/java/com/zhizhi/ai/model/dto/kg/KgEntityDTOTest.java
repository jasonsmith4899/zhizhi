package com.zhizhi.ai.model.dto.kg;

import com.zhizhi.ai.model.entity.KgEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KgEntityDTO 单元测试")
class KgEntityDTOTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        KgEntityDTO dto = new KgEntityDTO();
        dto.setId(1L);
        dto.setName("张三");
        dto.setType("人物");
        dto.setDescription("CEO");
        dto.setMentionCount(5);
        LocalDateTime now = LocalDateTime.now();
        dto.setCreatedAt(now);

        assertEquals(1L, dto.getId());
        assertEquals("张三", dto.getName());
        assertEquals("人物", dto.getType());
        assertEquals("CEO", dto.getDescription());
        assertEquals(5, dto.getMentionCount());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        KgEntityDTO dto = new KgEntityDTO(1L, "公司", "组织", "desc", 3, now);

        assertEquals(1L, dto.getId());
        assertEquals("公司", dto.getName());
        assertEquals("组织", dto.getType());
    }

    @Test
    @DisplayName("fromEntity 正确映射")
    void fromEntity() {
        KgEntity entity = KgEntity.builder()
                .id(1L)
                .name("实体")
                .type("概念")
                .description("描述")
                .mentionCount(10)
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .build();

        KgEntityDTO dto = KgEntityDTO.fromEntity(entity);

        assertEquals(1L, dto.getId());
        assertEquals("实体", dto.getName());
        assertEquals("概念", dto.getType());
        assertEquals("描述", dto.getDescription());
        assertEquals(10, dto.getMentionCount());
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), dto.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        KgEntityDTO d1 = new KgEntityDTO(1L, "A", "T", "D", 1, null);
        KgEntityDTO d2 = new KgEntityDTO(1L, "A", "T", "D", 1, null);
        KgEntityDTO d3 = new KgEntityDTO(2L, "B", "T", "D", 1, null);

        assertEquals(d1, d2);
        assertNotEquals(d1, d3);
    }
}

package com.zhizhi.ai.model.dto.kg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KgEntityDetailDTO 单元测试")
class KgEntityDetailDTOTest {

    @Test
    @DisplayName("继承 KgEntityDTO 字段")
    void inheritsFields() {
        KgEntityDetailDTO dto = new KgEntityDetailDTO();
        dto.setId(1L);
        dto.setName("实体");
        dto.setType("类型");
        dto.setDescription("描述");
        dto.setMentionCount(5);

        assertEquals(1L, dto.getId());
        assertEquals("实体", dto.getName());
        assertEquals("类型", dto.getType());
        assertEquals("描述", dto.getDescription());
        assertEquals(5, dto.getMentionCount());
    }

    @Test
    @DisplayName("relations 字段 Getter/Setter")
    void relationsGetterSetter() {
        KgEntityDetailDTO.RelationInfo info = new KgEntityDetailDTO.RelationInfo();
        info.setId(1L);
        info.setPredicate("任职于");
        info.setDirection("out");
        info.setOtherEntity("公司A");
        info.setConfidence(0.9f);

        assertEquals(1L, info.getId());
        assertEquals("任职于", info.getPredicate());
        assertEquals("out", info.getDirection());
        assertEquals("公司A", info.getOtherEntity());
        assertEquals(0.9f, info.getConfidence());
    }

    @Test
    @DisplayName("RelationInfo AllArgsConstructor")
    void relationInfoAllArgsConstructor() {
        KgEntityDetailDTO.RelationInfo info = new KgEntityDetailDTO.RelationInfo(
                1L, "属于", "in", "实体B", 0.8f);

        assertEquals(1L, info.getId());
        assertEquals("属于", info.getPredicate());
        assertEquals("in", info.getDirection());
        assertEquals("实体B", info.getOtherEntity());
        assertEquals(0.8f, info.getConfidence());
    }

    @Test
    @DisplayName("KgEntityDetailDTO AllArgsConstructor")
    void allArgsConstructor() {
        List<KgEntityDetailDTO.RelationInfo> relations = List.of(
                new KgEntityDetailDTO.RelationInfo(1L, "p1", "out", "e1", 0.9f)
        );
        KgEntityDetailDTO dto = new KgEntityDetailDTO();
        dto.setId(1L);
        dto.setName("entity");
        dto.setRelations(relations);

        assertEquals(1L, dto.getId());
        assertEquals(1, dto.getRelations().size());
    }

    @Test
    @DisplayName("RelationInfo equals/hashCode")
    void relationInfoEqualsHashCode() {
        KgEntityDetailDTO.RelationInfo r1 = new KgEntityDetailDTO.RelationInfo(
                1L, "p", "out", "e", 0.5f);
        KgEntityDetailDTO.RelationInfo r2 = new KgEntityDetailDTO.RelationInfo(
                1L, "p", "out", "e", 0.5f);
        KgEntityDetailDTO.RelationInfo r3 = new KgEntityDetailDTO.RelationInfo(
                2L, "p", "in", "e", 0.5f);

        assertEquals(r1, r2);
        assertNotEquals(r1, r3);
    }
}

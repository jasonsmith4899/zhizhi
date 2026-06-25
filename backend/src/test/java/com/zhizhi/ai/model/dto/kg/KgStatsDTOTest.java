package com.zhizhi.ai.model.dto.kg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KgStatsDTO 单元测试")
class KgStatsDTOTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        KgStatsDTO dto = new KgStatsDTO();
        dto.setEntityCount(100L);
        dto.setRelationCount(200L);
        dto.setAvgMentionCount(5);
        List<KgStatsDTO.TopEntity> topEntities = List.of(
                new KgStatsDTO.TopEntity("实体A", "人物", 10)
        );
        List<KgStatsDTO.TypeCount> typeDistribution = List.of(
                new KgStatsDTO.TypeCount("人物", 50L)
        );
        dto.setTopEntities(topEntities);
        dto.setTypeDistribution(typeDistribution);

        assertEquals(100L, dto.getEntityCount());
        assertEquals(200L, dto.getRelationCount());
        assertEquals(5, dto.getAvgMentionCount());
        assertEquals(1, dto.getTopEntities().size());
        assertEquals(1, dto.getTypeDistribution().size());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        List<KgStatsDTO.TopEntity> topEntities = List.of();
        List<KgStatsDTO.TypeCount> typeDist = List.of();
        KgStatsDTO dto = new KgStatsDTO(100L, 200L, 5, topEntities, typeDist);

        assertEquals(100L, dto.getEntityCount());
        assertEquals(200L, dto.getRelationCount());
    }

    @Test
    @DisplayName("TopEntity Getter/Setter")
    void topEntityGetterSetter() {
        KgStatsDTO.TopEntity entity = new KgStatsDTO.TopEntity();
        entity.setName("实体A");
        entity.setType("人物");
        entity.setMentionCount(10);

        assertEquals("实体A", entity.getName());
        assertEquals("人物", entity.getType());
        assertEquals(10, entity.getMentionCount());
    }

    @Test
    @DisplayName("TopEntity AllArgsConstructor")
    void topEntityAllArgsConstructor() {
        KgStatsDTO.TopEntity entity = new KgStatsDTO.TopEntity("A", "T", 5);

        assertEquals("A", entity.getName());
        assertEquals("T", entity.getType());
        assertEquals(5, entity.getMentionCount());
    }

    @Test
    @DisplayName("TypeCount Getter/Setter")
    void typeCountGetterSetter() {
        KgStatsDTO.TypeCount tc = new KgStatsDTO.TypeCount();
        tc.setType("组织");
        tc.setCount(30L);

        assertEquals("组织", tc.getType());
        assertEquals(30L, tc.getCount());
    }

    @Test
    @DisplayName("TypeCount AllArgsConstructor")
    void typeCountAllArgsConstructor() {
        KgStatsDTO.TypeCount tc = new KgStatsDTO.TypeCount("概念", 15L);

        assertEquals("概念", tc.getType());
        assertEquals(15L, tc.getCount());
    }

    @Test
    @DisplayName("TopEntity equals/hashCode")
    void topEntityEqualsHashCode() {
        KgStatsDTO.TopEntity e1 = new KgStatsDTO.TopEntity("A", "T", 5);
        KgStatsDTO.TopEntity e2 = new KgStatsDTO.TopEntity("A", "T", 5);
        KgStatsDTO.TopEntity e3 = new KgStatsDTO.TopEntity("B", "T", 3);

        assertEquals(e1, e2);
        assertNotEquals(e1, e3);
    }

    @Test
    @DisplayName("TypeCount equals/hashCode")
    void typeCountEqualsHashCode() {
        KgStatsDTO.TypeCount t1 = new KgStatsDTO.TypeCount("A", 10L);
        KgStatsDTO.TypeCount t2 = new KgStatsDTO.TypeCount("A", 10L);
        KgStatsDTO.TypeCount t3 = new KgStatsDTO.TypeCount("B", 20L);

        assertEquals(t1, t2);
        assertNotEquals(t1, t3);
    }
}

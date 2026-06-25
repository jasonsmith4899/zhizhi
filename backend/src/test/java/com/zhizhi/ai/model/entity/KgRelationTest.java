package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KgRelation 实体单元测试")
class KgRelationTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        KgRelation rel = new KgRelation();
        rel.setId(1L);
        rel.setTenantId(10L);
        rel.setKnowledgeBaseId(20L);
        rel.setSourceId(100L);
        rel.setTargetId(200L);
        rel.setPredicate("任职于");
        rel.setDocumentId(50L);
        rel.setConfidence(0.95f);
        LocalDateTime now = LocalDateTime.now();
        rel.setCreatedAt(now);

        assertEquals(1L, rel.getId());
        assertEquals(10L, rel.getTenantId());
        assertEquals(20L, rel.getKnowledgeBaseId());
        assertEquals(100L, rel.getSourceId());
        assertEquals(200L, rel.getTargetId());
        assertEquals("任职于", rel.getPredicate());
        assertEquals(50L, rel.getDocumentId());
        assertEquals(0.95f, rel.getConfidence());
        assertEquals(now, rel.getCreatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        KgRelation rel = new KgRelation(1L, 10L, 20L, 100L, 200L,
                "属于", 50L, 0.8f, now);

        assertEquals(1L, rel.getId());
        assertEquals("属于", rel.getPredicate());
        assertEquals(0.8f, rel.getConfidence());
    }

    @Test
    @DisplayName("Builder 验证默认值 confidence=1.0")
    void builderDefaults() {
        KgRelation rel = KgRelation.builder().build();
        assertEquals(1.0f, rel.getConfidence());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt")
    void prePersist() {
        KgRelation rel = KgRelation.builder().build();
        rel.onCreate();
        assertNotNull(rel.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        KgRelation r1 = KgRelation.builder().id(1L).predicate("p1").build();
        KgRelation r2 = KgRelation.builder().id(1L).predicate("p1").build();
        KgRelation r3 = KgRelation.builder().id(2L).predicate("p2").build();

        assertEquals(r1, r2);
        assertNotEquals(r1, r3);
    }
}

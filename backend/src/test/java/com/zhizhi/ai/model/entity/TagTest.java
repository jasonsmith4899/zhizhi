package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tag 实体单元测试")
class TagTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setTenantId(10L);
        tag.setKnowledgeBaseId(20L);
        tag.setName("重要");
        tag.setColor("#FF0000");
        LocalDateTime now = LocalDateTime.now();
        tag.setCreatedAt(now);

        assertEquals(1L, tag.getId());
        assertEquals(10L, tag.getTenantId());
        assertEquals(20L, tag.getKnowledgeBaseId());
        assertEquals("重要", tag.getName());
        assertEquals("#FF0000", tag.getColor());
        assertEquals(now, tag.getCreatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Tag tag = new Tag(1L, 10L, 20L, "标签", "#00FF00", now);

        assertEquals(1L, tag.getId());
        assertEquals("标签", tag.getName());
        assertEquals("#00FF00", tag.getColor());
    }

    @Test
    @DisplayName("Builder")
    void builder() {
        Tag tag = Tag.builder()
                .id(1L).tenantId(10L).knowledgeBaseId(20L)
                .name("test").color("#000").build();

        assertEquals(1L, tag.getId());
        assertEquals("test", tag.getName());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt")
    void prePersist() {
        Tag tag = Tag.builder().build();
        tag.onCreate();
        assertNotNull(tag.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        Tag t1 = Tag.builder().id(1L).name("tag1").build();
        Tag t2 = Tag.builder().id(1L).name("tag1").build();
        Tag t3 = Tag.builder().id(2L).name("tag2").build();

        assertEquals(t1, t2);
        assertNotEquals(t1, t3);
    }
}

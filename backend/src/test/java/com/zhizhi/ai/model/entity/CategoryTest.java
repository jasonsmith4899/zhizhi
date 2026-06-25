package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Category 实体单元测试")
class CategoryTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        Category cat = new Category();
        cat.setId(1L);
        cat.setTenantId(10L);
        cat.setKnowledgeBaseId(20L);
        cat.setParentId(null);
        cat.setName("技术文档");
        cat.setSortOrder(5);
        LocalDateTime now = LocalDateTime.now();
        cat.setCreatedAt(now);

        assertEquals(1L, cat.getId());
        assertEquals(10L, cat.getTenantId());
        assertEquals(20L, cat.getKnowledgeBaseId());
        assertNull(cat.getParentId());
        assertEquals("技术文档", cat.getName());
        assertEquals(5, cat.getSortOrder());
        assertEquals(now, cat.getCreatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Category cat = new Category(1L, 10L, 20L, 5L, "子分类", 0, now);

        assertEquals(1L, cat.getId());
        assertEquals(5L, cat.getParentId());
        assertEquals("子分类", cat.getName());
    }

    @Test
    @DisplayName("Builder 验证默认值 sortOrder=0")
    void builderDefaults() {
        Category cat = Category.builder().build();
        assertEquals(0, cat.getSortOrder());
    }

    @Test
    @DisplayName("Builder 自定义值")
    void builderCustom() {
        Category cat = Category.builder()
                .id(1L).tenantId(10L).knowledgeBaseId(20L)
                .parentId(5L).name("自定义").sortOrder(3).build();

        assertEquals(1L, cat.getId());
        assertEquals(5L, cat.getParentId());
        assertEquals("自定义", cat.getName());
        assertEquals(3, cat.getSortOrder());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt")
    void prePersist() {
        Category cat = Category.builder().build();
        cat.onCreate();
        assertNotNull(cat.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        Category cat1 = Category.builder().id(1L).name("cat1").build();
        Category cat2 = Category.builder().id(1L).name("cat1").build();
        Category cat3 = Category.builder().id(2L).name("cat2").build();

        assertEquals(cat1, cat2);
        assertNotEquals(cat1, cat3);
    }
}

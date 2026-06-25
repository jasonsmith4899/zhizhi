package com.zhizhi.ai.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CategoryRequest 单元测试")
class CategoryRequestTest {

    @Test
    @DisplayName("Getter/Setter")
    void getterSetter() {
        CategoryRequest req = new CategoryRequest();
        req.setKnowledgeBaseId(10L);
        req.setParentId(5L);
        req.setName("分类名");
        req.setSortOrder(3);

        assertEquals(10L, req.getKnowledgeBaseId());
        assertEquals(5L, req.getParentId());
        assertEquals("分类名", req.getName());
        assertEquals(3, req.getSortOrder());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        CategoryRequest r1 = new CategoryRequest();
        r1.setName("cat1");
        r1.setKnowledgeBaseId(10L);

        CategoryRequest r2 = new CategoryRequest();
        r2.setName("cat1");
        r2.setKnowledgeBaseId(10L);

        assertEquals(r1, r2);
    }
}

package com.zhizhi.ai.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TagRequest 单元测试")
class TagRequestTest {

    @Test
    @DisplayName("Getter/Setter")
    void getterSetter() {
        TagRequest req = new TagRequest();
        req.setKnowledgeBaseId(10L);
        req.setName("标签");
        req.setColor("#FF0000");

        assertEquals(10L, req.getKnowledgeBaseId());
        assertEquals("标签", req.getName());
        assertEquals("#FF0000", req.getColor());
    }

    @Test
    @DisplayName("color 可以为 null")
    void colorNull() {
        TagRequest req = new TagRequest();
        req.setName("test");
        assertNull(req.getColor());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        TagRequest r1 = new TagRequest();
        r1.setName("tag1");
        r1.setColor("#000");

        TagRequest r2 = new TagRequest();
        r2.setName("tag1");
        r2.setColor("#000");

        assertEquals(r1, r2);
    }
}

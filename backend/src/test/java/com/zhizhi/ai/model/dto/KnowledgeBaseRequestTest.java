package com.zhizhi.ai.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KnowledgeBaseRequest 单元测试")
class KnowledgeBaseRequestTest {

    @Test
    @DisplayName("Getter/Setter")
    void getterSetter() {
        KnowledgeBaseRequest req = new KnowledgeBaseRequest();
        req.setName("知识库名称");
        req.setDescription("描述");

        assertEquals("知识库名称", req.getName());
        assertEquals("描述", req.getDescription());
    }

    @Test
    @DisplayName("description 可以为 null")
    void descriptionNull() {
        KnowledgeBaseRequest req = new KnowledgeBaseRequest();
        req.setName("test");
        assertNull(req.getDescription());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        KnowledgeBaseRequest r1 = new KnowledgeBaseRequest();
        r1.setName("kb1");
        r1.setDescription("d1");

        KnowledgeBaseRequest r2 = new KnowledgeBaseRequest();
        r2.setName("kb1");
        r2.setDescription("d1");

        assertEquals(r1, r2);
    }
}

package com.zhizhi.ai.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DocumentProcessEvent 单元测试")
class DocumentProcessEventTest {

    @Test
    @DisplayName("Record 构造器和访问器")
    void constructorAndAccessors() {
        byte[] content = {1, 2, 3, 4, 5};
        DocumentProcessEvent event = new DocumentProcessEvent(
                1L, content, "test.pdf", 10L);

        assertEquals(1L, event.documentId());
        assertArrayEquals(content, event.contentBytes());
        assertEquals("test.pdf", event.filename());
        assertEquals(10L, event.knowledgeBaseId());
    }

    @Test
    @DisplayName("contentBytes 可以为 null")
    void contentBytesNull() {
        DocumentProcessEvent event = new DocumentProcessEvent(
                1L, null, "test.pdf", 10L);

        assertNull(event.contentBytes());
    }

    @Test
    @DisplayName("equals 基于值比较")
    void equals() {
        byte[] content = {1, 2, 3};
        DocumentProcessEvent e1 = new DocumentProcessEvent(1L, content, "f.pdf", 10L);
        DocumentProcessEvent e2 = new DocumentProcessEvent(1L, content, "f.pdf", 10L);

        assertEquals(e1, e2);
    }

    @Test
    @DisplayName("equals 不同值不相等")
    void notEquals() {
        byte[] content = {1, 2, 3};
        DocumentProcessEvent e1 = new DocumentProcessEvent(1L, content, "f.pdf", 10L);
        DocumentProcessEvent e2 = new DocumentProcessEvent(2L, content, "f.pdf", 10L);

        assertNotEquals(e1, e2);
    }

    @Test
    @DisplayName("hashCode 基于值")
    void hashCodeConsistent() {
        byte[] content = {1, 2, 3};
        DocumentProcessEvent e1 = new DocumentProcessEvent(1L, content, "f.pdf", 10L);
        DocumentProcessEvent e2 = new DocumentProcessEvent(1L, content, "f.pdf", 10L);

        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    @DisplayName("toString 包含字段名")
    void toStringContainsFields() {
        DocumentProcessEvent event = new DocumentProcessEvent(
                1L, new byte[]{1}, "test.pdf", 10L);

        String str = event.toString();
        assertTrue(str.contains("documentId"));
        assertTrue(str.contains("filename"));
        assertTrue(str.contains("knowledgeBaseId"));
    }
}

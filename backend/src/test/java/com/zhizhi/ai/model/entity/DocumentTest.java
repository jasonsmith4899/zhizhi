package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Document 实体单元测试")
class DocumentTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        Document doc = new Document();
        doc.setId(1L);
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(20L);
        doc.setKnowledgeBase(kb);
        doc.setTenantId(10L);
        doc.setFilename("test.pdf");
        doc.setFileType("pdf");
        doc.setFileSize(1024L);
        doc.setContent("文档内容");
        doc.setChunkCount(5);
        doc.setStatus("ready");
        doc.setErrorMessage(null);
        doc.setContentHash("abc123");
        doc.setMimeType("application/pdf");
        doc.setCategoryId(100L);
        LocalDateTime now = LocalDateTime.now();
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);

        assertEquals(1L, doc.getId());
        assertEquals(kb, doc.getKnowledgeBase());
        assertEquals(10L, doc.getTenantId());
        assertEquals("test.pdf", doc.getFilename());
        assertEquals("pdf", doc.getFileType());
        assertEquals(1024L, doc.getFileSize());
        assertEquals("文档内容", doc.getContent());
        assertEquals(5, doc.getChunkCount());
        assertEquals("ready", doc.getStatus());
        assertNull(doc.getErrorMessage());
        assertEquals("abc123", doc.getContentHash());
        assertEquals("application/pdf", doc.getMimeType());
        assertEquals(100L, doc.getCategoryId());
        assertEquals(now, doc.getCreatedAt());
        assertEquals(now, doc.getUpdatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(20L);
        LocalDateTime now = LocalDateTime.now();
        Document doc = new Document(1L, kb, 10L, "f.txt", "txt", 500L,
                "content", 3, "ready", null, "hash", "text/plain", 100L, now, now);

        assertEquals(1L, doc.getId());
        assertEquals("f.txt", doc.getFilename());
        assertEquals("ready", doc.getStatus());
    }

    @Test
    @DisplayName("Builder 验证默认值")
    void builderDefaults() {
        Document doc = Document.builder().build();
        assertEquals(0, doc.getChunkCount());
        assertEquals("processing", doc.getStatus());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt 和 updatedAt")
    void prePersist() {
        Document doc = Document.builder().build();
        doc.onCreate();
        assertNotNull(doc.getCreatedAt());
        assertNotNull(doc.getUpdatedAt());
    }

    @Test
    @DisplayName("@PreUpdate 更新 updatedAt")
    void preUpdate() {
        Document doc = Document.builder().build();
        doc.onCreate();
        LocalDateTime original = doc.getCreatedAt();

        doc.onUpdate();
        assertNotNull(doc.getUpdatedAt());
        assertEquals(original, doc.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        Document doc1 = Document.builder().id(1L).filename("a.pdf").build();
        Document doc2 = Document.builder().id(1L).filename("a.pdf").build();
        Document doc3 = Document.builder().id(2L).filename("b.pdf").build();

        assertEquals(doc1, doc2);
        assertNotEquals(doc1, doc3);
    }
}

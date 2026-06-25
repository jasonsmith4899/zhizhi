package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DocumentChunk 实体单元测试")
class DocumentChunkTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(1L);
        chunk.setDocumentId(10L);
        chunk.setKnowledgeBaseId(20L);
        chunk.setTenantId(30L);
        chunk.setChunkIndex(0);
        chunk.setContent("这是第一段内容");
        chunk.setContentLength(7);
        chunk.setVectorId("vec-001");
        LocalDateTime now = LocalDateTime.now();
        chunk.setCreatedAt(now);

        assertEquals(1L, chunk.getId());
        assertEquals(10L, chunk.getDocumentId());
        assertEquals(20L, chunk.getKnowledgeBaseId());
        assertEquals(30L, chunk.getTenantId());
        assertEquals(0, chunk.getChunkIndex());
        assertEquals("这是第一段内容", chunk.getContent());
        assertEquals(7, chunk.getContentLength());
        assertEquals("vec-001", chunk.getVectorId());
        assertEquals(now, chunk.getCreatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        DocumentChunk chunk = new DocumentChunk(1L, 10L, 20L, 30L, 2,
                "内容", 2, "vec-002", now);

        assertEquals(1L, chunk.getId());
        assertEquals(2, chunk.getChunkIndex());
        assertEquals("内容", chunk.getContent());
    }

    @Test
    @DisplayName("Builder 自定义值")
    void builderCustom() {
        DocumentChunk chunk = DocumentChunk.builder()
                .id(1L).documentId(10L).knowledgeBaseId(20L).tenantId(30L)
                .chunkIndex(0).content("test").build();

        assertEquals(1L, chunk.getId());
        assertEquals("test", chunk.getContent());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt 且自动计算 contentLength")
    void prePersist_calculatesContentLength() {
        DocumentChunk chunk = DocumentChunk.builder()
                .content("Hello World 你好世界")
                .build();
        chunk.onCreate();

        assertNotNull(chunk.getCreatedAt());
        assertEquals("Hello World 你好世界".length(), chunk.getContentLength());
    }

    @Test
    @DisplayName("@PrePersist content 为 null 时不设置 contentLength")
    void prePersist_nullContent() {
        DocumentChunk chunk = DocumentChunk.builder().build();
        chunk.onCreate();

        assertNotNull(chunk.getCreatedAt());
        assertNull(chunk.getContentLength());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        DocumentChunk c1 = DocumentChunk.builder().id(1L).documentId(10L).build();
        DocumentChunk c2 = DocumentChunk.builder().id(1L).documentId(10L).build();
        DocumentChunk c3 = DocumentChunk.builder().id(2L).documentId(20L).build();

        assertEquals(c1, c2);
        assertNotEquals(c1, c3);
    }
}

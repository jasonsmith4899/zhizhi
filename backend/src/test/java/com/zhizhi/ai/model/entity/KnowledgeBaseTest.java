package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KnowledgeBase 实体单元测试")
class KnowledgeBaseTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setUserId(10L);
        kb.setTenantId(20L);
        kb.setName("我的知识库");
        kb.setDescription("描述");
        kb.setStatus("archived");
        kb.setDocumentCount(5);
        kb.setChunkCount(50);
        LocalDateTime now = LocalDateTime.now();
        kb.setCreatedAt(now);
        kb.setUpdatedAt(now);
        List<Document> docs = new ArrayList<>();
        kb.setDocuments(docs);

        assertEquals(1L, kb.getId());
        assertEquals(10L, kb.getUserId());
        assertEquals(20L, kb.getTenantId());
        assertEquals("我的知识库", kb.getName());
        assertEquals("描述", kb.getDescription());
        assertEquals("archived", kb.getStatus());
        assertEquals(5, kb.getDocumentCount());
        assertEquals(50, kb.getChunkCount());
        assertEquals(now, kb.getCreatedAt());
        assertEquals(now, kb.getUpdatedAt());
        assertEquals(docs, kb.getDocuments());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        List<Document> docs = new ArrayList<>();
        KnowledgeBase kb = new KnowledgeBase(1L, 10L, 20L, "kb", "desc",
                "active", 3, 30, now, now, docs);

        assertEquals(1L, kb.getId());
        assertEquals("kb", kb.getName());
    }

    @Test
    @DisplayName("Builder 验证默认值")
    void builderDefaults() {
        KnowledgeBase kb = KnowledgeBase.builder().build();
        assertEquals("active", kb.getStatus());
        assertEquals(0, kb.getDocumentCount());
        assertEquals(0, kb.getChunkCount());
        assertNotNull(kb.getDocuments());
        assertTrue(kb.getDocuments().isEmpty());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt 和 updatedAt")
    void prePersist() {
        KnowledgeBase kb = KnowledgeBase.builder().build();
        kb.onCreate();
        assertNotNull(kb.getCreatedAt());
        assertNotNull(kb.getUpdatedAt());
    }

    @Test
    @DisplayName("@PreUpdate 更新 updatedAt")
    void preUpdate() {
        KnowledgeBase kb = KnowledgeBase.builder().build();
        kb.onCreate();
        LocalDateTime original = kb.getCreatedAt();

        kb.onUpdate();
        assertNotNull(kb.getUpdatedAt());
        assertEquals(original, kb.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        KnowledgeBase kb1 = KnowledgeBase.builder().id(1L).name("kb1").build();
        KnowledgeBase kb2 = KnowledgeBase.builder().id(1L).name("kb1").build();
        KnowledgeBase kb3 = KnowledgeBase.builder().id(2L).name("kb2").build();

        assertEquals(kb1, kb2);
        assertNotEquals(kb1, kb3);
    }
}

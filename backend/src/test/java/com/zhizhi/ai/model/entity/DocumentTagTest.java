package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DocumentTag 实体单元测试")
class DocumentTagTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        DocumentTag dt = new DocumentTag();
        dt.setDocumentId(1L);
        dt.setTagId(2L);
        dt.setTenantId(10L);

        assertEquals(1L, dt.getDocumentId());
        assertEquals(2L, dt.getTagId());
        assertEquals(10L, dt.getTenantId());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        DocumentTag dt = new DocumentTag(1L, 2L, 10L);

        assertEquals(1L, dt.getDocumentId());
        assertEquals(2L, dt.getTagId());
        assertEquals(10L, dt.getTenantId());
    }

    @Test
    @DisplayName("Builder")
    void builder() {
        DocumentTag dt = DocumentTag.builder()
                .documentId(1L).tagId(2L).tenantId(10L).build();

        assertEquals(1L, dt.getDocumentId());
        assertEquals(2L, dt.getTagId());
    }

    @Test
    @DisplayName("复合主键 PK 构造和 Getter/Setter")
    void pkGetterSetter() {
        DocumentTag.PK pk = new DocumentTag.PK();
        pk.setDocumentId(1L);
        pk.setTagId(2L);

        assertEquals(1L, pk.getDocumentId());
        assertEquals(2L, pk.getTagId());
    }

    @Test
    @DisplayName("复合主键 PK AllArgsConstructor")
    void pkAllArgsConstructor() {
        DocumentTag.PK pk = new DocumentTag.PK(1L, 2L);

        assertEquals(1L, pk.getDocumentId());
        assertEquals(2L, pk.getTagId());
    }

    @Test
    @DisplayName("复合主键 PK equals/hashCode")
    void pkEqualsHashCode() {
        DocumentTag.PK pk1 = new DocumentTag.PK(1L, 2L);
        DocumentTag.PK pk2 = new DocumentTag.PK(1L, 2L);
        DocumentTag.PK pk3 = new DocumentTag.PK(1L, 3L);

        assertEquals(pk1, pk2);
        assertEquals(pk1.hashCode(), pk2.hashCode());
        assertNotEquals(pk1, pk3);
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        DocumentTag dt1 = DocumentTag.builder().documentId(1L).tagId(2L).build();
        DocumentTag dt2 = DocumentTag.builder().documentId(1L).tagId(2L).build();
        DocumentTag dt3 = DocumentTag.builder().documentId(1L).tagId(3L).build();

        assertEquals(dt1, dt2);
        assertNotEquals(dt1, dt3);
    }
}

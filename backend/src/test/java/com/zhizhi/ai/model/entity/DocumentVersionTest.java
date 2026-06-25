package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DocumentVersion 实体单元测试")
class DocumentVersionTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        DocumentVersion ver = new DocumentVersion();
        ver.setId(1L);
        ver.setDocumentId(10L);
        ver.setTenantId(20L);
        ver.setVersionNo(3);
        ver.setContent("版本内容");
        ver.setChunkCount(5);
        ver.setCreatedBy(100L);
        ver.setRemark("手动修改");
        LocalDateTime now = LocalDateTime.now();
        ver.setCreatedAt(now);

        assertEquals(1L, ver.getId());
        assertEquals(10L, ver.getDocumentId());
        assertEquals(20L, ver.getTenantId());
        assertEquals(3, ver.getVersionNo());
        assertEquals("版本内容", ver.getContent());
        assertEquals(5, ver.getChunkCount());
        assertEquals(100L, ver.getCreatedBy());
        assertEquals("手动修改", ver.getRemark());
        assertEquals(now, ver.getCreatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        DocumentVersion ver = new DocumentVersion(1L, 10L, 20L, 1,
                "content", 3, 100L, "remark", now);

        assertEquals(1L, ver.getId());
        assertEquals(1, ver.getVersionNo());
    }

    @Test
    @DisplayName("Builder")
    void builder() {
        DocumentVersion ver = DocumentVersion.builder()
                .id(1L).documentId(10L).tenantId(20L).versionNo(1).build();

        assertEquals(1L, ver.getId());
        assertEquals(1, ver.getVersionNo());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt")
    void prePersist() {
        DocumentVersion ver = DocumentVersion.builder().build();
        ver.onCreate();
        assertNotNull(ver.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        DocumentVersion v1 = DocumentVersion.builder().id(1L).versionNo(1).build();
        DocumentVersion v2 = DocumentVersion.builder().id(1L).versionNo(1).build();
        DocumentVersion v3 = DocumentVersion.builder().id(2L).versionNo(2).build();

        assertEquals(v1, v2);
        assertNotEquals(v1, v3);
    }
}

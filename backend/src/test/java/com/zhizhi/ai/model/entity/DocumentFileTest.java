package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DocumentFile 实体单元测试")
class DocumentFileTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        DocumentFile file = new DocumentFile();
        file.setDocumentId(1L);
        file.setTenantId(10L);
        file.setData(new byte[]{1, 2, 3});
        file.setFileSize(3L);
        LocalDateTime now = LocalDateTime.now();
        file.setCreatedAt(now);

        assertEquals(1L, file.getDocumentId());
        assertEquals(10L, file.getTenantId());
        assertArrayEquals(new byte[]{1, 2, 3}, file.getData());
        assertEquals(3L, file.getFileSize());
        assertEquals(now, file.getCreatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        byte[] data = {10, 20, 30};
        LocalDateTime now = LocalDateTime.now();
        DocumentFile file = new DocumentFile(1L, 10L, data, 3L, now);

        assertEquals(1L, file.getDocumentId());
        assertArrayEquals(data, file.getData());
    }

    @Test
    @DisplayName("Builder")
    void builder() {
        DocumentFile file = DocumentFile.builder()
                .documentId(1L).tenantId(10L)
                .data(new byte[]{5}).fileSize(1L).build();

        assertEquals(1L, file.getDocumentId());
        assertEquals(1L, file.getFileSize());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt")
    void prePersist() {
        DocumentFile file = DocumentFile.builder().build();
        file.onCreate();
        assertNotNull(file.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        DocumentFile f1 = DocumentFile.builder().documentId(1L).tenantId(10L).build();
        DocumentFile f2 = DocumentFile.builder().documentId(1L).tenantId(10L).build();
        DocumentFile f3 = DocumentFile.builder().documentId(2L).tenantId(20L).build();

        assertEquals(f1, f2);
        assertNotEquals(f1, f3);
    }
}

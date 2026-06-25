package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditLog 实体单元测试")
class AuditLogTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setTenantId(10L);
        log.setUserId(20L);
        log.setAction("CREATE_DOCUMENT");
        log.setTargetType("document");
        log.setTargetId(100L);
        log.setDetail("创建文档 test.pdf");
        log.setIp("127.0.0.1");
        log.setSuccess(true);
        LocalDateTime now = LocalDateTime.now();
        log.setCreatedAt(now);

        assertEquals(1L, log.getId());
        assertEquals(10L, log.getTenantId());
        assertEquals(20L, log.getUserId());
        assertEquals("CREATE_DOCUMENT", log.getAction());
        assertEquals("document", log.getTargetType());
        assertEquals(100L, log.getTargetId());
        assertEquals("创建文档 test.pdf", log.getDetail());
        assertEquals("127.0.0.1", log.getIp());
        assertTrue(log.getSuccess());
        assertEquals(now, log.getCreatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        AuditLog log = new AuditLog(1L, 10L, 20L, "DELETE", "kb", 50L,
                "删除知识库", "192.168.1.1", false, now);

        assertEquals(1L, log.getId());
        assertEquals("DELETE", log.getAction());
        assertFalse(log.getSuccess());
    }

    @Test
    @DisplayName("Builder 验证默认值 success=true")
    void builderDefaults() {
        AuditLog log = AuditLog.builder().build();
        assertTrue(log.getSuccess());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt")
    void prePersist() {
        AuditLog log = AuditLog.builder().build();
        log.onCreate();
        assertNotNull(log.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        LocalDateTime now = LocalDateTime.now();
        AuditLog log1 = AuditLog.builder().id(1L).action("CREATE").success(true).createdAt(now).build();
        AuditLog log2 = AuditLog.builder().id(1L).action("CREATE").success(true).createdAt(now).build();
        AuditLog log3 = AuditLog.builder().id(2L).action("DELETE").success(false).createdAt(now).build();

        assertEquals(log1, log2);
        assertEquals(log1.hashCode(), log2.hashCode());
        assertNotEquals(log1, log3);
    }
}

package com.zhizhi.ai.service;

import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.entity.AuditLog;
import com.zhizhi.ai.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    // ---------- record ----------

    @Test
    void record_success() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.record(10L, 1L, "LOGIN", "user", 1L, "login success", "127.0.0.1", true);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog log = captor.getValue();
        assertEquals(10L, log.getTenantId());
        assertEquals(1L, log.getUserId());
        assertEquals("LOGIN", log.getAction());
        assertEquals("user", log.getTargetType());
        assertEquals(1L, log.getTargetId());
        assertEquals("login success", log.getDetail());
        assertEquals("127.0.0.1", log.getIp());
        assertTrue(log.getSuccess());
    }

    @Test
    void record_blankTargetType_savedAsNull() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.record(10L, 1L, "DELETE", "  ", 5L, null, null, true);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertNull(captor.getValue().getTargetType());
    }

    @Test
    void record_nullTargetType_savedAsNull() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.record(10L, 1L, "DELETE", null, 5L, null, null, true);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertNull(captor.getValue().getTargetType());
    }

    @Test
    void record_longDetail_truncatedTo1000() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        String longDetail = "x".repeat(2000);
        auditService.record(10L, 1L, "UPDATE", "document", 3L, longDetail, "10.0.0.1", true);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(1000, captor.getValue().getDetail().length());
    }

    @Test
    void record_failureSuccessFlag() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.record(10L, 1L, "UPLOAD", "document", 3L, "failed", "10.0.0.1", false);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertFalse(captor.getValue().getSuccess());
    }

    @Test
    void record_repositoryException_doesNotThrow() {
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("DB error"));

        // Should not throw — best-effort recording
        assertDoesNotThrow(() ->
                auditService.record(10L, 1L, "LOGIN", "user", 1L, null, null, true));
    }

    @Test
    void record_exactBoundaryDetail_1000Chars() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        String detail1000 = "y".repeat(1000);
        auditService.record(10L, 1L, "ACTION", "target", 1L, detail1000, null, true);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(1000, captor.getValue().getDetail().length());
        assertEquals(detail1000, captor.getValue().getDetail());
    }

    // ---------- list ----------

    @Test
    void list_success() {
        TenantContext.setTenantId(10L);
        try {
            Page<AuditLog> page = new PageImpl<>(List.of(
                    AuditLog.builder().tenantId(10L).action("LOGIN").build(),
                    AuditLog.builder().tenantId(10L).action("DELETE").build()
            ));
            when(auditLogRepository.findByTenantIdOrderByCreatedAtDesc(eq(10L), any(PageRequest.class)))
                    .thenReturn(page);

            Page<AuditLog> result = auditService.list(0, 10);

            assertEquals(2, result.getContent().size());
            verify(auditLogRepository).findByTenantIdOrderByCreatedAtDesc(10L, PageRequest.of(0, 10));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void list_emptyResult() {
        TenantContext.setTenantId(10L);
        try {
            Page<AuditLog> emptyPage = new PageImpl<>(List.of());
            when(auditLogRepository.findByTenantIdOrderByCreatedAtDesc(eq(10L), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            Page<AuditLog> result = auditService.list(0, 10);
            assertTrue(result.getContent().isEmpty());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void list_passesPaginationParams() {
        TenantContext.setTenantId(10L);
        try {
            Page<AuditLog> page = new PageImpl<>(List.of());
            when(auditLogRepository.findByTenantIdOrderByCreatedAtDesc(eq(10L), any(PageRequest.class)))
                    .thenReturn(page);

            auditService.list(2, 25);

            verify(auditLogRepository).findByTenantIdOrderByCreatedAtDesc(10L, PageRequest.of(2, 25));
        } finally {
            TenantContext.clear();
        }
    }
}

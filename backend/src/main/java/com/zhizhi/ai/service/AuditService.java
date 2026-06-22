package com.zhizhi.ai.service;

import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.entity.AuditLog;
import com.zhizhi.ai.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 异步记录审计日志。上下文（tenantId/userId/ip）须由调用方在原线程提取后传入，
     * 因为 @Async 切换线程后 ThreadLocal（TenantContext/SecurityContext）不可用。
     */
    @Async
    public void record(Long tenantId, Long userId, String action, String targetType,
                       Long targetId, String detail, String ip, boolean success) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .action(action)
                    .targetType(targetType == null || targetType.isBlank() ? null : targetType)
                    .targetId(targetId)
                    .detail(detail != null && detail.length() > 1000 ? detail.substring(0, 1000) : detail)
                    .ip(ip)
                    .success(success)
                    .build());
        } catch (Exception e) {
            log.warn("审计日志保存失败: action={}, err={}", action, e.getMessage());
        }
    }

    /** 查询当前租户的审计日志（分页） */
    public Page<AuditLog> list(int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size));
    }
}

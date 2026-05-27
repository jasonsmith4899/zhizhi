package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    List<KnowledgeBase> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, String status);
    List<KnowledgeBase> findByUserIdOrderByUpdatedAtDesc(Long userId);
    long countByUserId(Long userId);

    // 租户隔离查询方法
    List<KnowledgeBase> findByTenantIdAndStatusOrderByUpdatedAtDesc(Long tenantId, String status);
    List<KnowledgeBase> findByTenantIdOrderByUpdatedAtDesc(Long tenantId);
    long countByTenantId(Long tenantId);
}

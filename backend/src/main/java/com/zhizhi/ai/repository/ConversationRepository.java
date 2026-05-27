package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findBySessionId(String sessionId);
    Page<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
    long countByUserId(Long userId);

    // 租户隔离查询方法
    Optional<Conversation> findByTenantIdAndSessionId(Long tenantId, String sessionId);
    Page<Conversation> findByTenantIdOrderByUpdatedAtDesc(Long tenantId, Pageable pageable);
    long countByTenantId(Long tenantId);
}

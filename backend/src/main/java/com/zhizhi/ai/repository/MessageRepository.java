package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId ORDER BY m.createdAt DESC LIMIT :limit")
    List<Message> findRecentByConversationId(Long conversationId, int limit);

    long countByConversationId(Long conversationId);

    // 租户隔离查询方法
    Page<Message> findByTenantIdAndConversationIdOrderByCreatedAtAsc(Long tenantId, Long conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.tenantId = :tenantId AND m.conversationId = :conversationId ORDER BY m.createdAt DESC LIMIT :limit")
    List<Message> findRecentByTenantIdAndConversationId(Long tenantId, Long conversationId, int limit);

    long countByTenantIdAndConversationId(Long tenantId, Long conversationId);
}

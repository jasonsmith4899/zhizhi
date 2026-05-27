package com.zhizhi.ai.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 消息实体
 */
@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 20)
    private String role;  // user / assistant / system

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "source_documents", columnDefinition = "TEXT")
    private String sourceDocuments;  // JSON格式的来源引用

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

package com.zhizhi.ai.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * API Key 实体 - 一个用户可以有多个 Key，每个 Key 可关联不同知识库
 */
@Entity
@Table(name = "api_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "key_value", unique = true, nullable = false, length = 64)
    private String keyValue;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String name = "默认Key";

    @Column(length = 500)
    private String description;

    @Column(name = "assistant_persona", columnDefinition = "TEXT")
    private String assistantPersona;

    @Column(name = "merchant_background", columnDefinition = "TEXT")
    private String merchantBackground;

    @Column(name = "answer_rules", columnDefinition = "TEXT")
    private String answerRules;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 关联的知识库 ID 列表（多对多）
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "api_key_knowledge_bases",
        joinColumns = @JoinColumn(name = "api_key_id"),
        inverseJoinColumns = @JoinColumn(name = "knowledge_base_id")
    )
    @Builder.Default
    private Set<KnowledgeBase> knowledgeBases = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 获取关联的知识库 ID 集合
     */
    public Set<Long> getKnowledgeBaseIds() {
        if (knowledgeBases == null) return Set.of();
        return knowledgeBases.stream()
                .map(KnowledgeBase::getId)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 检查是否有权访问指定知识库
     */
    public boolean canAccessKnowledgeBase(Long knowledgeBaseId) {
        if (knowledgeBases == null || knowledgeBases.isEmpty()) return false;
        return getKnowledgeBaseIds().contains(knowledgeBaseId);
    }
}

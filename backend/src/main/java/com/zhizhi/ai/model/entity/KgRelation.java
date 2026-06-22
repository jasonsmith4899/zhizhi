package com.zhizhi.ai.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * KAG 知识图谱 - 关系三元组 (source -[predicate]-> target)
 */
@Entity
@Table(name = "kg_relations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KgRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "knowledge_base_id", nullable = false)
    private Long knowledgeBaseId;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 100)
    private String predicate;  // 关系谓词

    @Column(name = "document_id")
    private Long documentId;

    @Column
    @Builder.Default
    private Float confidence = 1.0f;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

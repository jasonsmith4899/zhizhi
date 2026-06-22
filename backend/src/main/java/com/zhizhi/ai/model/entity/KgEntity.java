package com.zhizhi.ai.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * KAG 知识图谱 - 实体节点
 */
@Entity
@Table(name = "kg_entities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "knowledge_base_id", nullable = false)
    private Long knowledgeBaseId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50)
    private String type;  // 人物/组织/产品/概念...

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "norm_name", nullable = false, length = 200)
    private String normName;  // 归一化名，用于去重对齐

    @Column(name = "mention_count")
    @Builder.Default
    private Integer mentionCount = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

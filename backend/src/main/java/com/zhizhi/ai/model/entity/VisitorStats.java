package com.zhizhi.ai.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 访客统计实体
 */
@Entity
@Table(name = "visitor_stats", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitorStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "total_visitors")
    @Builder.Default
    private Integer totalVisitors = 0;

    @Column(name = "total_queries")
    @Builder.Default
    private Integer totalQueries = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

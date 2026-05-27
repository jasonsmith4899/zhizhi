package com.zhizhi.ai.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 租户/商户实体
 */
@Entity
@Table(name = "tenants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String logo;

    @Column(length = 200)
    private String domain;

    @Column(length = 50)
    @Builder.Default
    private String plan = "free";  // free / basic / pro / enterprise

    @Column(length = 50)
    @Builder.Default
    private String status = "active";  // active / suspended

    @Column(name = "max_documents")
    @Builder.Default
    private Integer maxDocuments = 10;

    @Column(name = "max_daily_queries")
    @Builder.Default
    private Integer maxDailyQueries = 100;

    @Column(name = "welcome_message", length = 1000)
    private String welcomeMessage;

    @Column(name = "prompt_template", columnDefinition = "TEXT")
    private String promptTemplate;

    @Column(name = "wechat_appid", length = 64)
    private String wechatAppid;

    @JsonIgnore
    @Column(name = "wechat_secret", length = 128)
    private String wechatSecret;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

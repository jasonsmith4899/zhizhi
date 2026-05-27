package com.zhizhi.ai.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(unique = true, nullable = false, length = 200)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 50)
    @Builder.Default
    private String plan = "free";  // free / basic / pro

    @Column(name = "api_key", unique = true, length = 64)
    private String apiKey;

    @Column(name = "api_key_created_at")
    private LocalDateTime apiKeyCreatedAt;

    @Column(name = "refresh_token_version")
    @Builder.Default
    private Integer refreshTokenVersion = 0;

    @Column(name = "daily_queries_used")
    @Builder.Default
    private Integer dailyQueriesUsed = 0;

    @Column(name = "daily_queries_reset_at")
    private LocalDateTime dailyQueriesResetAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private boolean newUser;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        dailyQueriesResetAt = LocalDateTime.now().toLocalDate().atStartOfDay().plusDays(1);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VisitorStats 实体单元测试")
class VisitorStatsTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        VisitorStats stats = new VisitorStats();
        stats.setId(1L);
        stats.setTenantId(10L);
        stats.setDate(LocalDate.of(2024, 1, 15));
        stats.setTotalVisitors(100);
        stats.setTotalQueries(500);
        LocalDateTime now = LocalDateTime.now();
        stats.setCreatedAt(now);

        assertEquals(1L, stats.getId());
        assertEquals(10L, stats.getTenantId());
        assertEquals(LocalDate.of(2024, 1, 15), stats.getDate());
        assertEquals(100, stats.getTotalVisitors());
        assertEquals(500, stats.getTotalQueries());
        assertEquals(now, stats.getCreatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDate date = LocalDate.of(2024, 6, 1);
        LocalDateTime now = LocalDateTime.now();
        VisitorStats stats = new VisitorStats(1L, 10L, date, 50, 200, now);

        assertEquals(1L, stats.getId());
        assertEquals(date, stats.getDate());
        assertEquals(50, stats.getTotalVisitors());
        assertEquals(200, stats.getTotalQueries());
    }

    @Test
    @DisplayName("Builder 验证默认值")
    void builderDefaults() {
        VisitorStats stats = VisitorStats.builder().build();
        assertEquals(0, stats.getTotalVisitors());
        assertEquals(0, stats.getTotalQueries());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt")
    void prePersist() {
        VisitorStats stats = VisitorStats.builder().build();
        stats.onCreate();
        assertNotNull(stats.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        LocalDate date = LocalDate.of(2024, 1, 1);
        VisitorStats s1 = VisitorStats.builder().id(1L).tenantId(10L).date(date).build();
        VisitorStats s2 = VisitorStats.builder().id(1L).tenantId(10L).date(date).build();
        VisitorStats s3 = VisitorStats.builder().id(2L).tenantId(20L).date(date).build();

        assertEquals(s1, s2);
        assertNotEquals(s1, s3);
    }
}

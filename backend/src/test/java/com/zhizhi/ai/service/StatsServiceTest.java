package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.model.entity.VisitorStats;
import com.zhizhi.ai.repository.ConversationRepository;
import com.zhizhi.ai.repository.MessageRepository;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.VisitorStatsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private VisitorStatsRepository visitorStatsRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private TenantMemberRepository tenantMemberRepository;

    @InjectMocks
    private StatsService statsService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupAuthentication(Long userId) {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(userId);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ==================== recordQuery ====================

    @Test
    void recordQuery_newStats_createsAndSaves() {
        setupAuthentication(1L);
        when(tenantMemberRepository.existsByTenantIdAndUserId(1L, 1L)).thenReturn(true);
        when(visitorStatsRepository.findByTenantIdAndDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(visitorStatsRepository.save(any(VisitorStats.class))).thenAnswer(invocation -> {
            VisitorStats stats = invocation.getArgument(0);
            stats.setId(1L);
            return stats;
        });

        statsService.recordQuery(1L);

        verify(visitorStatsRepository).save(argThat(stats ->
                stats.getTenantId().equals(1L)
                        && stats.getTotalQueries() == 1
                        && stats.getDate().equals(LocalDate.now())
        ));
    }

    @Test
    void recordQuery_existingStats_incrementsQueryCount() {
        setupAuthentication(1L);
        when(tenantMemberRepository.existsByTenantIdAndUserId(1L, 1L)).thenReturn(true);
        VisitorStats existing = VisitorStats.builder()
                .id(1L).tenantId(1L).date(LocalDate.now())
                .totalQueries(5).totalVisitors(3).build();
        when(visitorStatsRepository.findByTenantIdAndDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));
        when(visitorStatsRepository.save(any(VisitorStats.class))).thenAnswer(invocation -> invocation.getArgument(0));

        statsService.recordQuery(1L);

        verify(visitorStatsRepository).save(argThat(stats -> stats.getTotalQueries() == 6));
    }

    @Test
    void recordQuery_noAccess_throwsForbidden() {
        setupAuthentication(1L);
        when(tenantMemberRepository.existsByTenantIdAndUserId(1L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> statsService.recordQuery(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(403));
    }

    @Test
    void recordQuery_noAuth_skipsValidation() {
        // No authentication set -> auth is null -> validation passes silently
        when(visitorStatsRepository.findByTenantIdAndDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(visitorStatsRepository.save(any(VisitorStats.class))).thenAnswer(invocation -> invocation.getArgument(0));

        statsService.recordQuery(1L);

        verify(tenantMemberRepository, never()).existsByTenantIdAndUserId(anyLong(), anyLong());
        verify(visitorStatsRepository).save(any(VisitorStats.class));
    }

    // ==================== recordVisitor ====================

    @Test
    void recordVisitor_newStats_createsAndSaves() {
        setupAuthentication(1L);
        when(tenantMemberRepository.existsByTenantIdAndUserId(1L, 1L)).thenReturn(true);
        when(visitorStatsRepository.findByTenantIdAndDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(visitorStatsRepository.save(any(VisitorStats.class))).thenAnswer(invocation -> {
            VisitorStats stats = invocation.getArgument(0);
            stats.setId(1L);
            return stats;
        });

        statsService.recordVisitor(1L);

        verify(visitorStatsRepository).save(argThat(stats ->
                stats.getTotalVisitors() == 1
                        && stats.getTotalQueries() == 0
        ));
    }

    @Test
    void recordVisitor_existingStats_incrementsVisitorCount() {
        setupAuthentication(1L);
        when(tenantMemberRepository.existsByTenantIdAndUserId(1L, 1L)).thenReturn(true);
        VisitorStats existing = VisitorStats.builder()
                .id(1L).tenantId(1L).date(LocalDate.now())
                .totalQueries(10).totalVisitors(7).build();
        when(visitorStatsRepository.findByTenantIdAndDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));
        when(visitorStatsRepository.save(any(VisitorStats.class))).thenAnswer(invocation -> invocation.getArgument(0));

        statsService.recordVisitor(1L);

        verify(visitorStatsRepository).save(argThat(stats -> stats.getTotalVisitors() == 8));
    }

    @Test
    void recordVisitor_noAccess_throwsForbidden() {
        setupAuthentication(1L);
        when(tenantMemberRepository.existsByTenantIdAndUserId(1L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> statsService.recordVisitor(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(403));
    }

    // ==================== getDashboard ====================

    @Test
    void getDashboard_returnsAggregatedStats() {
        LocalDate today = LocalDate.now();
        List<VisitorStats> stats = List.of(
                VisitorStats.builder().id(1L).tenantId(1L).date(today)
                        .totalVisitors(10).totalQueries(20).build(),
                VisitorStats.builder().id(2L).tenantId(1L).date(today.minusDays(1))
                        .totalVisitors(5).totalQueries(15).build(),
                VisitorStats.builder().id(3L).tenantId(1L).date(today.minusDays(5))
                        .totalVisitors(3).totalQueries(8).build()
        );
        when(visitorStatsRepository.findByTenantIdAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(stats);

        Map<String, Object> dashboard = statsService.getDashboard(1L);

        assertThat(dashboard.get("totalVisitors30d")).isEqualTo(18);  // 10+5+3
        assertThat(dashboard.get("totalQueries30d")).isEqualTo(43);   // 20+15+8
        assertThat(dashboard.get("todayVisitors")).isEqualTo(10);
        assertThat(dashboard.get("todayQueries")).isEqualTo(20);
        assertThat(dashboard.get("dailyStats")).isEqualTo(stats);
    }

    @Test
    void getDashboard_noTodayStats_returnsZeroForToday() {
        LocalDate today = LocalDate.now();
        List<VisitorStats> stats = List.of(
                VisitorStats.builder().id(1L).tenantId(1L).date(today.minusDays(1))
                        .totalVisitors(5).totalQueries(10).build()
        );
        when(visitorStatsRepository.findByTenantIdAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(stats);

        Map<String, Object> dashboard = statsService.getDashboard(1L);

        assertThat(dashboard.get("totalVisitors30d")).isEqualTo(5);
        assertThat(dashboard.get("totalQueries30d")).isEqualTo(10);
        assertThat(dashboard.get("todayVisitors")).isEqualTo(0);
        assertThat(dashboard.get("todayQueries")).isEqualTo(0);
    }

    @Test
    void getDashboard_emptyStats_returnsAllZeros() {
        when(visitorStatsRepository.findByTenantIdAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        Map<String, Object> dashboard = statsService.getDashboard(1L);

        assertThat(dashboard.get("totalVisitors30d")).isEqualTo(0);
        assertThat(dashboard.get("totalQueries30d")).isEqualTo(0);
        assertThat(dashboard.get("todayVisitors")).isEqualTo(0);
        assertThat(dashboard.get("todayQueries")).isEqualTo(0);
        assertThat((List<?>) dashboard.get("dailyStats")).isEmpty();
    }
}

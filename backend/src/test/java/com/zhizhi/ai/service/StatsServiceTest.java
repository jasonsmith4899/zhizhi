package com.zhizhi.ai.service;

import com.zhizhi.ai.model.entity.VisitorStats;
import com.zhizhi.ai.repository.ConversationRepository;
import com.zhizhi.ai.repository.MessageRepository;
import com.zhizhi.ai.repository.VisitorStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatsService 单元测试")
class StatsServiceTest {

    @InjectMocks
    private StatsService statsService;

    @Mock
    private VisitorStatsRepository visitorStatsRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    private final Long TENANT_ID = 1L;

    @Test
    @DisplayName("记录查询 - 新建统计记录")
    void recordQuery_newRecord() {
        when(visitorStatsRepository.findByTenantIdAndDate(eq(TENANT_ID), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(visitorStatsRepository.save(any(VisitorStats.class))).thenAnswer(i -> i.getArgument(0));

        statsService.recordQuery(TENANT_ID);

        verify(visitorStatsRepository).save(argThat(s ->
                s.getTotalQueries() == 1 && s.getTenantId().equals(TENANT_ID)));
    }

    @Test
    @DisplayName("记录查询 - 累加已有记录")
    void recordQuery_increment() {
        VisitorStats existing = VisitorStats.builder()
                .tenantId(TENANT_ID)
                .date(LocalDate.now())
                .totalQueries(5)
                .totalVisitors(3)
                .build();

        when(visitorStatsRepository.findByTenantIdAndDate(eq(TENANT_ID), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));
        when(visitorStatsRepository.save(any(VisitorStats.class))).thenAnswer(i -> i.getArgument(0));

        statsService.recordQuery(TENANT_ID);

        verify(visitorStatsRepository).save(argThat(s -> s.getTotalQueries() == 6));
    }

    @Test
    @DisplayName("记录访客")
    void recordVisitor() {
        when(visitorStatsRepository.findByTenantIdAndDate(eq(TENANT_ID), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(visitorStatsRepository.save(any(VisitorStats.class))).thenAnswer(i -> i.getArgument(0));

        statsService.recordVisitor(TENANT_ID);

        verify(visitorStatsRepository).save(argThat(s ->
                s.getTotalVisitors() == 1 && s.getTenantId().equals(TENANT_ID)));
    }

    @Test
    @DisplayName("获取仪表盘数据")
    void getDashboard() {
        VisitorStats today = VisitorStats.builder()
                .tenantId(TENANT_ID)
                .date(LocalDate.now())
                .totalVisitors(10)
                .totalQueries(50)
                .build();

        when(visitorStatsRepository.findByTenantIdAndDateRange(eq(TENANT_ID), any(), any()))
                .thenReturn(List.of(today));

        Map<String, Object> dashboard = statsService.getDashboard(TENANT_ID);

        assertThat(dashboard).containsKey("totalVisitors30d");
        assertThat(dashboard).containsKey("totalQueries30d");
        assertThat(dashboard).containsKey("todayVisitors");
        assertThat(dashboard).containsKey("todayQueries");
        assertThat(dashboard.get("todayVisitors")).isEqualTo(10);
        assertThat(dashboard.get("todayQueries")).isEqualTo(50);
    }
}

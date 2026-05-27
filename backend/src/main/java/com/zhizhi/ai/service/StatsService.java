package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.entity.VisitorStats;
import com.zhizhi.ai.repository.ConversationRepository;
import com.zhizhi.ai.repository.MessageRepository;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.VisitorStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final VisitorStatsRepository visitorStatsRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final TenantMemberRepository tenantMemberRepository;

    /**
     * 记录查询（增加当日查询数）
     */
    @Transactional
    public void recordQuery(Long tenantId) {
        validateTenantAccess(tenantId);
        LocalDate today = LocalDate.now();
        VisitorStats stats = visitorStatsRepository.findByTenantIdAndDate(tenantId, today)
                .orElse(VisitorStats.builder()
                        .tenantId(tenantId)
                        .date(today)
                        .build());
        stats.setTotalQueries(stats.getTotalQueries() + 1);
        visitorStatsRepository.save(stats);
    }

    /**
     * 记录访客
     */
    @Transactional
    public void recordVisitor(Long tenantId) {
        validateTenantAccess(tenantId);
        LocalDate today = LocalDate.now();
        VisitorStats stats = visitorStatsRepository.findByTenantIdAndDate(tenantId, today)
                .orElse(VisitorStats.builder()
                        .tenantId(tenantId)
                        .date(today)
                        .build());
        stats.setTotalVisitors(stats.getTotalVisitors() + 1);
        visitorStatsRepository.save(stats);
    }

    private void validateTenantAccess(Long tenantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Long userId) {
            if (!tenantMemberRepository.existsByTenantIdAndUserId(tenantId, userId)) {
                throw BusinessException.forbidden("无权访问该租户数据");
            }
        }
    }

    /**
     * 获取租户仪表盘数据
     */
    public Map<String, Object> getDashboard(Long tenantId) {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);

        // 30天统计
        List<VisitorStats> stats = visitorStatsRepository.findByTenantIdAndDateRange(
                tenantId, thirtyDaysAgo, today);

        int totalVisitors = stats.stream().mapToInt(VisitorStats::getTotalVisitors).sum();
        int totalQueries = stats.stream().mapToInt(VisitorStats::getTotalQueries).sum();

        // 今日统计
        VisitorStats todayStats = stats.stream()
                .filter(s -> s.getDate().equals(today))
                .findFirst()
                .orElse(null);

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalVisitors30d", totalVisitors);
        dashboard.put("totalQueries30d", totalQueries);
        dashboard.put("todayVisitors", todayStats != null ? todayStats.getTotalVisitors() : 0);
        dashboard.put("todayQueries", todayStats != null ? todayStats.getTotalQueries() : 0);
        dashboard.put("dailyStats", stats);

        return dashboard;
    }
}

package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.VisitorStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VisitorStatsRepository extends JpaRepository<VisitorStats, Long> {

    Optional<VisitorStats> findByTenantIdAndDate(Long tenantId, LocalDate date);

    @Query("SELECT v FROM VisitorStats v WHERE v.tenantId = :tenantId AND v.date BETWEEN :start AND :end ORDER BY v.date")
    List<VisitorStats> findByTenantIdAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}

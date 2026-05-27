package com.zhizhi.ai.model.dto;

import com.zhizhi.ai.model.entity.Tenant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantDTO {

    private Long id;
    private String name;
    private String logo;
    private String domain;
    private String plan;
    private String status;
    private Integer maxDocuments;
    private Integer maxDailyQueries;
    private String welcomeMessage;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;

    public static TenantDTO fromEntity(Tenant tenant) {
        return TenantDTO.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .logo(tenant.getLogo())
                .domain(tenant.getDomain())
                .plan(tenant.getPlan())
                .status(tenant.getStatus())
                .maxDocuments(tenant.getMaxDocuments())
                .maxDailyQueries(tenant.getMaxDailyQueries())
                .welcomeMessage(tenant.getWelcomeMessage())
                .createdAt(tenant.getCreatedAt())
                .expiredAt(tenant.getExpiredAt())
                .build();
    }
}

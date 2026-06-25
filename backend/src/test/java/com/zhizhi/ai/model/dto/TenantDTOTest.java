package com.zhizhi.ai.model.dto;

import com.zhizhi.ai.model.entity.Tenant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantDTO 单元测试")
class TenantDTOTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        TenantDTO dto = new TenantDTO();
        dto.setId(1L);
        dto.setName("商户");
        dto.setLogo("logo.png");
        dto.setDomain("a.com");
        dto.setPlan("pro");
        dto.setStatus("active");
        dto.setMaxDocuments(100);
        dto.setMaxDailyQueries(1000);
        dto.setWelcomeMessage("欢迎");
        LocalDateTime now = LocalDateTime.now();
        dto.setCreatedAt(now);
        dto.setExpiredAt(now.plusDays(30));

        assertEquals(1L, dto.getId());
        assertEquals("商户", dto.getName());
        assertEquals("logo.png", dto.getLogo());
        assertEquals("a.com", dto.getDomain());
        assertEquals("pro", dto.getPlan());
        assertEquals("active", dto.getStatus());
        assertEquals(100, dto.getMaxDocuments());
        assertEquals(1000, dto.getMaxDailyQueries());
        assertEquals("欢迎", dto.getWelcomeMessage());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now.plusDays(30), dto.getExpiredAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        TenantDTO dto = new TenantDTO(1L, "商户", "logo", "domain",
                "free", "active", 10, 100, "welcome", now, now);

        assertEquals(1L, dto.getId());
        assertEquals("商户", dto.getName());
    }

    @Test
    @DisplayName("Builder")
    void builder() {
        TenantDTO dto = TenantDTO.builder().id(1L).name("t").build();
        assertEquals(1L, dto.getId());
    }

    @Test
    @DisplayName("fromEntity 正确映射")
    void fromEntity() {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .name("商户A")
                .logo("logo.png")
                .domain("a.com")
                .plan("pro")
                .status("active")
                .maxDocuments(50)
                .maxDailyQueries(500)
                .welcomeMessage("你好")
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .expiredAt(LocalDateTime.of(2024, 12, 31, 0, 0))
                .build();

        TenantDTO dto = TenantDTO.fromEntity(tenant);

        assertEquals(1L, dto.getId());
        assertEquals("商户A", dto.getName());
        assertEquals("logo.png", dto.getLogo());
        assertEquals("a.com", dto.getDomain());
        assertEquals("pro", dto.getPlan());
        assertEquals("active", dto.getStatus());
        assertEquals(50, dto.getMaxDocuments());
        assertEquals(500, dto.getMaxDailyQueries());
        assertEquals("你好", dto.getWelcomeMessage());
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2024, 12, 31, 0, 0), dto.getExpiredAt());
    }
}

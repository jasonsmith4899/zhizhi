package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tenant 实体单元测试")
class TenantTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("商户A");
        tenant.setLogo("logo.png");
        tenant.setDomain("a.example.com");
        tenant.setPlan("pro");
        tenant.setStatus("suspended");
        tenant.setMaxDocuments(100);
        tenant.setMaxDailyQueries(1000);
        tenant.setWelcomeMessage("欢迎");
        tenant.setPromptTemplate("模板");
        tenant.setWechatAppid("wx123");
        tenant.setWechatSecret("secret");
        LocalDateTime now = LocalDateTime.now();
        tenant.setCreatedAt(now);
        tenant.setExpiredAt(now.plusDays(30));

        assertEquals(1L, tenant.getId());
        assertEquals("商户A", tenant.getName());
        assertEquals("logo.png", tenant.getLogo());
        assertEquals("a.example.com", tenant.getDomain());
        assertEquals("pro", tenant.getPlan());
        assertEquals("suspended", tenant.getStatus());
        assertEquals(100, tenant.getMaxDocuments());
        assertEquals(1000, tenant.getMaxDailyQueries());
        assertEquals("欢迎", tenant.getWelcomeMessage());
        assertEquals("模板", tenant.getPromptTemplate());
        assertEquals("wx123", tenant.getWechatAppid());
        assertEquals("secret", tenant.getWechatSecret());
        assertEquals(now, tenant.getCreatedAt());
        assertEquals(now.plusDays(30), tenant.getExpiredAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Tenant tenant = new Tenant(1L, "商户", "logo", "domain",
                "free", "active", 10, 100, "welcome", "prompt",
                "appid", "secret", now, now.plusDays(30));

        assertEquals(1L, tenant.getId());
        assertEquals("商户", tenant.getName());
    }

    @Test
    @DisplayName("Builder 验证默认值")
    void builderDefaults() {
        Tenant tenant = Tenant.builder().build();
        assertEquals("free", tenant.getPlan());
        assertEquals("active", tenant.getStatus());
        assertEquals(10, tenant.getMaxDocuments());
        assertEquals(100, tenant.getMaxDailyQueries());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt")
    void prePersist() {
        Tenant tenant = Tenant.builder().build();
        tenant.onCreate();
        assertNotNull(tenant.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        Tenant t1 = Tenant.builder().id(1L).name("t1").build();
        Tenant t2 = Tenant.builder().id(1L).name("t1").build();
        Tenant t3 = Tenant.builder().id(2L).name("t2").build();

        assertEquals(t1, t2);
        assertNotEquals(t1, t3);
    }
}

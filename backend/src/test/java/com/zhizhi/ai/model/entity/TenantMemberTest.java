package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantMember 实体单元测试")
class TenantMemberTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        TenantMember member = new TenantMember();
        member.setId(1L);
        member.setTenantId(10L);
        member.setUserId(20L);
        member.setRole("admin");
        LocalDateTime now = LocalDateTime.now();
        member.setCreatedAt(now);

        assertEquals(1L, member.getId());
        assertEquals(10L, member.getTenantId());
        assertEquals(20L, member.getUserId());
        assertEquals("admin", member.getRole());
        assertEquals(now, member.getCreatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        TenantMember member = new TenantMember(1L, 10L, 20L, "owner", now);

        assertEquals(1L, member.getId());
        assertEquals("owner", member.getRole());
    }

    @Test
    @DisplayName("Builder 验证默认值 role=member")
    void builderDefaults() {
        TenantMember member = TenantMember.builder().build();
        assertEquals("member", member.getRole());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt")
    void prePersist() {
        TenantMember member = TenantMember.builder().build();
        member.onCreate();
        assertNotNull(member.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        TenantMember m1 = TenantMember.builder().id(1L).tenantId(10L).userId(20L).build();
        TenantMember m2 = TenantMember.builder().id(1L).tenantId(10L).userId(20L).build();
        TenantMember m3 = TenantMember.builder().id(2L).tenantId(10L).userId(30L).build();

        assertEquals(m1, m2);
        assertNotEquals(m1, m3);
    }
}

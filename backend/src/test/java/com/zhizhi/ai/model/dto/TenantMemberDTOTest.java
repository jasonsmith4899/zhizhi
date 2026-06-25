package com.zhizhi.ai.model.dto;

import com.zhizhi.ai.model.entity.TenantMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantMemberDTO 单元测试")
class TenantMemberDTOTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        TenantMemberDTO dto = new TenantMemberDTO();
        dto.setId(1L);
        dto.setUserId(10L);
        dto.setUsername("user1");
        dto.setRole("admin");
        LocalDateTime now = LocalDateTime.now();
        dto.setJoinedAt(now);

        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getUserId());
        assertEquals("user1", dto.getUsername());
        assertEquals("admin", dto.getRole());
        assertEquals(now, dto.getJoinedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        TenantMemberDTO dto = new TenantMemberDTO(1L, 10L, "user1", "owner", now);

        assertEquals(1L, dto.getId());
        assertEquals("user1", dto.getUsername());
        assertEquals("owner", dto.getRole());
    }

    @Test
    @DisplayName("Builder")
    void builder() {
        TenantMemberDTO dto = TenantMemberDTO.builder().id(1L).username("u").build();
        assertEquals(1L, dto.getId());
    }

    @Test
    @DisplayName("fromEntity 正确映射")
    void fromEntity() {
        TenantMember member = TenantMember.builder()
                .id(1L)
                .userId(10L)
                .role("admin")
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .build();

        TenantMemberDTO dto = TenantMemberDTO.fromEntity(member, "张三");

        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getUserId());
        assertEquals("张三", dto.getUsername());
        assertEquals("admin", dto.getRole());
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), dto.getJoinedAt());
    }
}

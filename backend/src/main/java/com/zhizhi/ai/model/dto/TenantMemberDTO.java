package com.zhizhi.ai.model.dto;

import com.zhizhi.ai.model.entity.TenantMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantMemberDTO {

    private Long id;
    private Long userId;
    private String username;
    private String role;
    private LocalDateTime joinedAt;

    public static TenantMemberDTO fromEntity(TenantMember member, String username) {
        return TenantMemberDTO.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .username(username)
                .role(member.getRole())
                .joinedAt(member.getCreatedAt())
                .build();
    }
}

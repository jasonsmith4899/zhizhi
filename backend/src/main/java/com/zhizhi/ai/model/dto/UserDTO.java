package com.zhizhi.ai.model.dto;

import com.zhizhi.ai.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private Long id;
    private String username;
    private String email;
    private String plan;
    private Integer dailyQueriesUsed;

    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .plan(user.getPlan())
                .dailyQueriesUsed(user.getDailyQueriesUsed())
                .build();
    }
}

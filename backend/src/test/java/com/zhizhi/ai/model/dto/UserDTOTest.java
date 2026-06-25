package com.zhizhi.ai.model.dto;

import com.zhizhi.ai.model.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserDTO 单元测试")
class UserDTOTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        UserDTO dto = new UserDTO();
        dto.setId(1L);
        dto.setUsername("user1");
        dto.setEmail("u1@e.com");
        dto.setPlan("basic");
        dto.setDailyQueriesUsed(10);

        assertEquals(1L, dto.getId());
        assertEquals("user1", dto.getUsername());
        assertEquals("u1@e.com", dto.getEmail());
        assertEquals("basic", dto.getPlan());
        assertEquals(10, dto.getDailyQueriesUsed());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        UserDTO dto = new UserDTO(1L, "user1", "u1@e.com", "free", 5);

        assertEquals(1L, dto.getId());
        assertEquals("user1", dto.getUsername());
    }

    @Test
    @DisplayName("Builder")
    void builder() {
        UserDTO dto = UserDTO.builder().id(1L).username("u").build();
        assertEquals(1L, dto.getId());
    }

    @Test
    @DisplayName("fromEntity 正确映射")
    void fromEntity() {
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@e.com")
                .plan("pro")
                .dailyQueriesUsed(25)
                .build();

        UserDTO dto = UserDTO.fromEntity(user);

        assertEquals(1L, dto.getId());
        assertEquals("testuser", dto.getUsername());
        assertEquals("test@e.com", dto.getEmail());
        assertEquals("pro", dto.getPlan());
        assertEquals(25, dto.getDailyQueriesUsed());
    }
}

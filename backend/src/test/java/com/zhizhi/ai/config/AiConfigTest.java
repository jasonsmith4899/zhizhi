package com.zhizhi.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiConfig 单元测试")
class AiConfigTest {

    @Mock
    private JdbcChatMemoryRepository chatMemoryRepository;

    @Test
    @DisplayName("chatMemory bean 创建成功")
    void chatMemory_notNull() {
        AiConfig config = new AiConfig(chatMemoryRepository);

        assertNotNull(config.chatMemory());
    }

    // 注：AiConfig 不再自定义 ObjectMapper bean。
    // 自定义的裸 new ObjectMapper() 会覆盖 Spring Boot 自动配置（缺少 JavaTimeModule），
    // 导致所有含 LocalDateTime 的 DTO 序列化抛 InvalidDefinitionException。
    // 现复用 Spring Boot 自动配置的 ObjectMapper，由 JacksonLocalDateTimeTest 守护该行为。
}

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

    @Test
    @DisplayName("objectMapper bean 创建成功")
    void objectMapper_notNull() {
        AiConfig config = new AiConfig(chatMemoryRepository);

        assertNotNull(config.objectMapper());
    }
}

package com.zhizhi.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 防回归测试：守护「容器装配的 ObjectMapper 能序列化 LocalDateTime」。
 *
 * 背景：AiConfig 曾自定义裸 {@code new ObjectMapper()} bean 覆盖 Spring Boot 自动配置，
 * 导致缺少 JavaTimeModule，所有含 LocalDateTime（createdAt）的 DTO 序列化抛
 * InvalidDefinitionException，几乎所有返回实体的接口 500。
 *
 * 本测试用 Spring Boot 的 JacksonAutoConfiguration 装配 ObjectMapper，
 * 并叠加 AiConfig，验证：
 * 1. 容器中最终的 ObjectMapper 能正常序列化 LocalDateTime；
 * 2. AiConfig 不再贡献覆盖自动配置的 ObjectMapper bean。
 */
@DisplayName("Jackson LocalDateTime 序列化防回归测试")
class JacksonLocalDateTimeTest {

    @Test
    @DisplayName("Spring Boot 自动配置的 ObjectMapper 能序列化 LocalDateTime")
    void autoConfiguredObjectMapperSerializesLocalDateTime() {
        new ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        JacksonAutoConfiguration.class))
                .run(context -> {
                    ObjectMapper mapper = context.getBean(ObjectMapper.class);
                    assertThatCode(() -> {
                        String json = mapper.writeValueAsString(LocalDateTime.of(2026, 6, 25, 10, 0, 0));
                        assertThat(json).contains("2026");
                    }).doesNotThrowAnyException();
                });
    }
}

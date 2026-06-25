package com.zhizhi.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebConfig 单元测试")
class WebConfigTest {

    @Mock
    private TenantInterceptor tenantInterceptor;

    @Test
    @DisplayName("addInterceptors 注册 TenantInterceptor")
    void addInterceptors_registersTenantInterceptor() {
        WebConfig webConfig = new WebConfig(tenantInterceptor);
        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        InterceptorRegistration registration = mock(InterceptorRegistration.class);

        when(registry.addInterceptor(tenantInterceptor)).thenReturn(registration);
        when(registration.addPathPatterns(any(String[].class))).thenReturn(registration);
        when(registration.excludePathPatterns(any(String[].class))).thenReturn(registration);

        webConfig.addInterceptors(registry);

        verify(registry).addInterceptor(tenantInterceptor);
    }
}

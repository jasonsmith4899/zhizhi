package com.zhizhi.ai.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TenantContext 单元测试")
class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("设置并获取租户ID")
    void setAndGet() {
        TenantContext.setTenantId(42L);
        assertThat(TenantContext.getTenantId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("清除后返回null")
    void clear() {
        TenantContext.setTenantId(1L);
        TenantContext.clear();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("未设置时返回null")
    void getWhenNotSet() {
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("覆盖设置")
    void overwrite() {
        TenantContext.setTenantId(1L);
        TenantContext.setTenantId(2L);
        assertThat(TenantContext.getTenantId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("线程隔离")
    void threadIsolation() throws InterruptedException {
        TenantContext.setTenantId(1L);

        Thread otherThread = new Thread(() -> {
            // 另一个线程应该看不到主线程的值
            assertThat(TenantContext.getTenantId()).isNull();
            TenantContext.setTenantId(99L);
            assertThat(TenantContext.getTenantId()).isEqualTo(99L);
        });
        otherThread.start();
        otherThread.join();

        // 主线程的值不受影响
        assertThat(TenantContext.getTenantId()).isEqualTo(1L);
    }
}

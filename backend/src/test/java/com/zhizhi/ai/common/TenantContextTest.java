package com.zhizhi.ai.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantContext ThreadLocal 测试")
class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("setTenantId 后 getTenantId 返回相同值")
    void setAndGet() {
        TenantContext.setTenantId(42L);
        assertEquals(42L, TenantContext.getTenantId());
    }

    @Test
    @DisplayName("未设置时 getTenantId 返回 null")
    void get_beforeSet_returnsNull() {
        assertNull(TenantContext.getTenantId());
    }

    @Test
    @DisplayName("clear 后 getTenantId 返回 null")
    void clear_removesValue() {
        TenantContext.setTenantId(1L);
        TenantContext.clear();
        assertNull(TenantContext.getTenantId());
    }

    @Test
    @DisplayName("set 可覆盖之前的值")
    void set_overwritesPrevious() {
        TenantContext.setTenantId(1L);
        TenantContext.setTenantId(2L);
        assertEquals(2L, TenantContext.getTenantId());
    }

    @Test
    @DisplayName("不同线程的 TenantContext 互相隔离")
    void threadIsolation() throws InterruptedException {
        TenantContext.setTenantId(100L);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong otherThreadValue = new AtomicLong(-1);

        Thread otherThread = new Thread(() -> {
            // 子线程未设置，应为 null
            otherThreadValue.set(TenantContext.getTenantId() == null ? -1 : TenantContext.getTenantId());
            TenantContext.setTenantId(200L);
            otherThreadValue.set(TenantContext.getTenantId());
            latch.countDown();
        });
        otherThread.start();
        latch.await();

        // 子线程设置的值不影响主线程
        assertEquals(100L, TenantContext.getTenantId());
        // 子线程读到了自己设置的值
        assertEquals(200L, otherThreadValue.get());
    }
}

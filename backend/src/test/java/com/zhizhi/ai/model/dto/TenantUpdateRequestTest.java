package com.zhizhi.ai.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantUpdateRequest 单元测试")
class TenantUpdateRequestTest {

    @Test
    @DisplayName("Getter/Setter")
    void getterSetter() {
        TenantUpdateRequest req = new TenantUpdateRequest();
        req.setName("新名称");
        req.setWechatAppId("wx123");

        assertEquals("新名称", req.getName());
        assertEquals("wx123", req.getWechatAppId());
    }

    @Test
    @DisplayName("字段可为 null")
    void nullFields() {
        TenantUpdateRequest req = new TenantUpdateRequest();
        assertNull(req.getName());
        assertNull(req.getWechatAppId());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        TenantUpdateRequest r1 = new TenantUpdateRequest();
        r1.setName("n1");
        r1.setWechatAppId("wx1");

        TenantUpdateRequest r2 = new TenantUpdateRequest();
        r2.setName("n1");
        r2.setWechatAppId("wx1");

        assertEquals(r1, r2);
    }
}

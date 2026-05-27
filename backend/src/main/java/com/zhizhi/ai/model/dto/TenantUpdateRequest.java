package com.zhizhi.ai.model.dto;

import lombok.Data;

@Data
public class TenantUpdateRequest {

    private String name;

    private String wechatAppId;
}

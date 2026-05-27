package com.zhizhi.ai.common;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 认证工具类 - 从Authentication中获取用户信息
 */
@Component
public class AuthUtil {

    /**
     * 从认证信息获取用户ID（从JWT解析后存入Authentication.details）
     */
    public Long getUserId(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof Long userId) {
            return userId;
        }
        throw BusinessException.unauthorized("无法获取用户信息");
    }
}

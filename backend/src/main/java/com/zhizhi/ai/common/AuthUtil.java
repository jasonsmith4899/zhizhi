package com.zhizhi.ai.common;

import com.zhizhi.ai.config.SecurityConfig.ApiKeyAuthDetail;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 认证工具类 - 从Authentication中获取用户信息
 */
@Component
public class AuthUtil {

    /**
     * 从认证信息获取用户ID
     * 支持两种认证方式：
     * - JWT: details 为 Long (userId)
     * - API Key: details 为 ApiKeyAuthDetail (userId, apiKeyId)
     */
    public Long getUserId(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof Long userId) {
            return userId;
        }
        if (details instanceof ApiKeyAuthDetail apiKeyDetail) {
            return apiKeyDetail.userId();
        }
        throw BusinessException.unauthorized("无法获取用户信息");
    }

    /**
     * 判断是否通过 API Key 认证
     */
    public boolean isApiKeyAuth(Authentication authentication) {
        return authentication.getDetails() instanceof ApiKeyAuthDetail;
    }

    /**
     * 获取 API Key ID（仅 API Key 认证时可用）
     */
    public Long getApiKeyId(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof ApiKeyAuthDetail apiKeyDetail) {
            return apiKeyDetail.apiKeyId();
        }
        return null;
    }
}

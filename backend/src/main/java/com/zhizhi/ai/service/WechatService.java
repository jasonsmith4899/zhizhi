package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.model.entity.Tenant;
import com.zhizhi.ai.model.entity.TenantMember;
import com.zhizhi.ai.model.entity.User;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.TenantRepository;
import com.zhizhi.ai.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * 微信小程序服务
 */
@Slf4j
@Service
public class WechatService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${spring.wechat.mini-program.default-appid:}")
    private String defaultAppId;

    @Value("${spring.wechat.mini-program.default-secret:}")
    private String defaultSecret;

    public WechatService(UserRepository userRepository,
                         TenantRepository tenantRepository,
                         TenantMemberRepository tenantMemberRepository,
                         JwtUtil jwtUtil,
                         ObjectMapper objectMapper,
                         RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.tenantMemberRepository = tenantMemberRepository;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    /**
     * 微信小程序登录 (code换openid + 创建/查找用户 + 生成JWT)
     */
    @Transactional
    public Map<String, Object> miniProgramLogin(String code, Long tenantId) {
        // 1. 获取租户的小程序配置（或使用默认配置）
        String appId = defaultAppId;
        String appSecret = defaultSecret;

        if (tenantId != null) {
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant != null && tenant.getWechatAppid() != null) {
                appId = tenant.getWechatAppid();
                appSecret = tenant.getWechatSecret();
            }
        }

        if (appId == null || appId.isEmpty()) {
            throw BusinessException.badRequest("小程序未配置AppID");
        }

        // 2. 调用微信code2session接口
        String openid = code2session(code, appId, appSecret);

        // 3. 查找或创建用户
        User user = userRepository.findByUsername("wx_" + openid)
                .orElseGet(() -> createUserByOpenid(openid, tenantId));

        // 4. 确保用户关联了租户
        Long resolvedTenantId = ensureTenantMember(user.getId(), tenantId);

        // 5. 生成JWT（嵌入tenantId）
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), resolvedTenantId);

        return Map.of(
                "token", token,
                "userId", user.getId(),
                "tenantId", resolvedTenantId,
                "isNew", user.isNewUser()
        );
    }

    /**
     * 调用微信code2session接口获取openid
     */
    private String code2session(String code, String appId, String appSecret) {
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appId, appSecret, code);

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode json = objectMapper.readTree(response);

            if (json.has("errcode") && json.get("errcode").asInt() != 0) {
                String errMsg = json.has("errmsg") ? json.get("errmsg").asText() : "未知错误";
                log.error("微信登录失败: {}", errMsg);
                throw BusinessException.badRequest("微信登录失败: " + errMsg);
            }

            return json.get("openid").asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信接口异常", e);
            throw BusinessException.badRequest("微信登录服务异常");
        }
    }

    /**
     * 根据openid创建小程序用户
     */
    private User createUserByOpenid(String openid, Long tenantId) {
        User user = User.builder()
                .username("wx_" + openid)
                .email("wx_" + openid + "@wechat.local")
                .password(UUID.randomUUID().toString()) // 随机密码，小程序用户不使用密码登录
                .plan("free")
                .build();
        user = userRepository.save(user);
        user.setNewUser(true);
        return user;
    }

    /**
     * 确保用户关联了租户，返回有效的tenantId
     */
    private Long ensureTenantMember(Long userId, Long tenantId) {
        // 已有租户关联，直接返回
        TenantMember existing = tenantMemberRepository.findByUserId(userId).orElse(null);
        if (existing != null) {
            return existing.getTenantId();
        }

        // 传入了tenantId，自动创建关联
        if (tenantId != null) {
            tenantRepository.findById(tenantId).orElseThrow(
                    () -> BusinessException.badRequest("租户不存在"));
            TenantMember member = TenantMember.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .role("member")
                    .build();
            tenantMemberRepository.save(member);
            return tenantId;
        }

        // 既没有关联也没有传入tenantId
        throw BusinessException.badRequest("用户未绑定租户，请联系管理员");
    }
}

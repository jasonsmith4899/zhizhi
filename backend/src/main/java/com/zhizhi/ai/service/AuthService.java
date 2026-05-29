package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.model.dto.LoginRequest;
import com.zhizhi.ai.model.dto.RegisterRequest;
import com.zhizhi.ai.model.dto.UserDTO;
import com.zhizhi.ai.model.dto.ApiKeyDTO;
import com.zhizhi.ai.model.entity.ApiKey;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import com.zhizhi.ai.model.entity.User;
import com.zhizhi.ai.repository.ApiKeyRepository;
import com.zhizhi.ai.repository.KnowledgeBaseRepository;
import com.zhizhi.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    public Map<String, Object> register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
        log.info("用户注册成功: {}", user.getUsername());

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getRefreshTokenVersion());

        return Map.of(
                "token", token,
                "refreshToken", refreshToken,
                "user", UserDTO.fromEntity(user)
        );
    }

    /**
     * 用户登录
     */
    public Map<String, Object> login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getRefreshTokenVersion());
        log.info("用户登录成功: {}", user.getUsername());

        return Map.of(
                "token", token,
                "refreshToken", refreshToken,
                "user", UserDTO.fromEntity(user)
        );
    }

    /**
     * 刷新access token（带token轮换）
     */
    @Transactional
    public Map<String, Object> refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BusinessException(401, "refresh token无效");
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));

        Integer tokenVersion = jwtUtil.getRefreshTokenVersion(refreshToken);
        if (tokenVersion != null && tokenVersion < user.getRefreshTokenVersion()) {
            throw new BusinessException(401, "refresh token已失效，请重新登录");
        }

        user.setRefreshTokenVersion(user.getRefreshTokenVersion() + 1);
        userRepository.save(user);

        String newToken = jwtUtil.generateToken(user.getId(), user.getUsername());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getRefreshTokenVersion());

        return Map.of(
                "token", newToken,
                "refreshToken", newRefreshToken
        );
    }

    /**
     * 获取当前用户信息
     */
    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("用户"));
    }

    // ==========================================
    // API Key 管理（支持多 Key + 关联知识库）
    // ==========================================

    /**
     * 获取用户的所有 API Key
     */
    public List<ApiKeyDTO> listApiKeys(Long userId) {
        return apiKeyRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50))
                .getContent()
                .stream()
                .map(ApiKeyDTO::fromEntityMasked)
                .toList();
    }

    /**
     * 创建新的 API Key
     */
    @Transactional
    public ApiKeyDTO createApiKey(Long userId, String name, String description, Set<Long> knowledgeBaseIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户"));

        String keyValue = UUID.randomUUID().toString().replace("-", "");

        ApiKey apiKey = ApiKey.builder()
                .userId(userId)
                .tenantId(null) // 可以从 TenantContext 获取
                .keyValue(keyValue)
                .name(name != null ? name : "默认Key")
                .description(description)
                .build();

        // 关联知识库（必须至少一个）
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            throw new BusinessException("必须至少关联一个知识库");
        }
        List<KnowledgeBase> kbs = knowledgeBaseRepository.findAllById(knowledgeBaseIds);
        // 校验知识库属于该用户
        Set<KnowledgeBase> validKbs = kbs.stream()
                .filter(kb -> kb.getUserId().equals(userId))
                .collect(Collectors.toSet());
        if (validKbs.isEmpty()) {
            throw new BusinessException("所选知识库无效");
        }
        apiKey.setKnowledgeBases(new HashSet<>(validKbs));

        apiKeyRepository.save(apiKey);
        log.info("用户 {} 创建了新的 API Key: {}", user.getUsername(), name);

        // 返回时使用完整 key（仅创建时返回一次）
        return ApiKeyDTO.fromEntity(apiKey);
    }

    /**
     * 更新 API Key（名称、描述、关联知识库）
     */
    @Transactional
    public ApiKeyDTO updateApiKey(Long userId, Long apiKeyId, String name, String description, Set<Long> knowledgeBaseIds) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> BusinessException.notFound("API Key"));

        if (!apiKey.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权操作此 API Key");
        }

        if (name != null) apiKey.setName(name);
        if (description != null) apiKey.setDescription(description);

        // 更新关联知识库
        if (knowledgeBaseIds != null) {
            List<KnowledgeBase> kbs = knowledgeBaseRepository.findAllById(knowledgeBaseIds);
            Set<KnowledgeBase> validKbs = kbs.stream()
                    .filter(kb -> kb.getUserId().equals(userId))
                    .collect(Collectors.toSet());
            apiKey.setKnowledgeBases(new HashSet<>(validKbs));
        }

        apiKeyRepository.save(apiKey);
        return ApiKeyDTO.fromEntityMasked(apiKey);
    }

    /**
     * 删除 API Key
     */
    @Transactional
    public void deleteApiKey(Long userId, Long apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> BusinessException.notFound("API Key"));

        if (!apiKey.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权操作此 API Key");
        }

        apiKeyRepository.delete(apiKey);
        log.info("用户 {} 删除了 API Key: {}", userId, apiKey.getName());
    }

    /**
     * 获取 API Key 详情（含完整 key 值，仅内部使用）
     */
    public ApiKey getApiKeyById(Long apiKeyId) {
        return apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> BusinessException.notFound("API Key"));
    }

    /**
     * 通过 key 值查找 API Key
     */
    public ApiKey getApiKeyByValue(String keyValue) {
        return apiKeyRepository.findByKeyValue(keyValue).orElse(null);
    }
}

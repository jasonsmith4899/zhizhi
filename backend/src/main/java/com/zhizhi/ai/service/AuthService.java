package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.model.dto.LoginRequest;
import com.zhizhi.ai.model.dto.RegisterRequest;
import com.zhizhi.ai.model.dto.UserDTO;
import com.zhizhi.ai.model.entity.User;
import com.zhizhi.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
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
                .apiKey(UUID.randomUUID().toString().replace("-", ""))
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

        // 校验token版本号，防止已轮换的token被重用
        Integer tokenVersion = jwtUtil.getRefreshTokenVersion(refreshToken);
        if (tokenVersion != null && tokenVersion < user.getRefreshTokenVersion()) {
            throw new BusinessException(401, "refresh token已失效，请重新登录");
        }

        // 递增版本号使旧token失效
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

    /**
     * 生成/重新生成API Key
     */
    public Map<String, Object> generateApiKey(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户"));

        String apiKey = UUID.randomUUID().toString().replace("-", "");
        user.setApiKey(apiKey);
        user.setApiKeyCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("用户 {} 生成了新的API Key", user.getUsername());
        return Map.of(
                "apiKey", apiKey,
                "apiKeyCreatedAt", user.getApiKeyCreatedAt().toString()
        );
    }

    /**
     * 吊销API Key
     */
    public void revokeApiKey(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户"));

        user.setApiKey(null);
        user.setApiKeyCreatedAt(null);
        userRepository.save(user);

        log.info("用户 {} 吊销了API Key", user.getUsername());
    }

    /**
     * 通过API Key查找用户
     */
    public User getUserByApiKey(String apiKey) {
        return userRepository.findByApiKey(apiKey)
                .orElse(null);
    }
}

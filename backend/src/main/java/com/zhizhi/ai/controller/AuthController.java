package com.zhizhi.ai.controller;

import com.zhizhi.ai.common.RateLimit;
import com.zhizhi.ai.common.Result;
import com.zhizhi.ai.model.dto.LoginRequest;
import com.zhizhi.ai.model.dto.RegisterRequest;
import com.zhizhi.ai.model.dto.UserDTO;
import com.zhizhi.ai.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @RateLimit(maxRequests = 5, windowSeconds = 60)
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        return Result.ok(authService.refreshToken(refreshToken));
    }

    @GetMapping("/me")
    public Result<UserDTO> me(Authentication authentication) {
        var user = authService.getCurrentUser(authentication.getName());
        return Result.ok(UserDTO.fromEntity(user));
    }
}

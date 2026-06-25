package com.zhizhi.ai.controller;

import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.Result;
import com.zhizhi.ai.model.dto.TenantDTO;
import com.zhizhi.ai.model.dto.TenantMemberDTO;
import com.zhizhi.ai.model.dto.TenantUpdateRequest;
import com.zhizhi.ai.repository.UserRepository;
import com.zhizhi.ai.service.StatsService;
import com.zhizhi.ai.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 租户管理API
 */
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final StatsService statsService;
    private final UserRepository userRepository;
    private final AuthUtil authUtil;

    @PostMapping
    public Result<TenantDTO> create(@RequestBody Map<String, String> request,
                                  Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        return Result.ok(TenantDTO.fromEntity(tenantService.create(request.get("name"), userId)));
    }

    @GetMapping("/mine")
    public Result<TenantDTO> getMyTenant(Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        return Result.ok(TenantDTO.fromEntity(tenantService.getByUserId(userId)));
    }

    @GetMapping("/{id}")
    public Result<TenantDTO> getById(@PathVariable Long id, Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        if (!tenantService.isMember(id, userId)) {
            throw BusinessException.forbidden("无权访问此租户");
        }
        return Result.ok(TenantDTO.fromEntity(tenantService.getById(id)));
    }

    @PutMapping("/{id}")
    public Result<TenantDTO> update(@PathVariable Long id,
                                  @RequestBody TenantUpdateRequest updateData,
                                  Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        return Result.ok(TenantDTO.fromEntity(tenantService.update(id, updateData, userId)));
    }

    @PostMapping("/{id}/members")
    public Result<TenantMemberDTO> addMember(@PathVariable Long id,
                                           @RequestBody Map<String, String> request,
                                           Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        var member = tenantService.addMember(id, request.get("username"),
                request.getOrDefault("role", "member"), userId);
        String username = userRepository.findById(member.getUserId())
                .map(u -> u.getUsername())
                .orElse("unknown");
        return Result.ok(TenantMemberDTO.fromEntity(member, username));
    }

    @GetMapping("/{id}/members")
    public Result<List<TenantMemberDTO>> getMembers(@PathVariable Long id,
                                                  Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        List<TenantMemberDTO> dtos = tenantService.getMembers(id, userId)
                .stream()
                .map(member -> {
                    String username = userRepository.findById(member.getUserId())
                            .map(u -> u.getUsername())
                            .orElse("unknown");
                    return TenantMemberDTO.fromEntity(member, username);
                })
                .toList();
        return Result.ok(dtos);
    }

    @GetMapping("/{id}/dashboard")
    public Result<Map<String, Object>> dashboard(@PathVariable Long id,
                                                  Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        if (!tenantService.isMember(id, userId)) {
            throw BusinessException.forbidden("无权操作");
        }
        return Result.ok(statsService.getDashboard(id));
    }
}

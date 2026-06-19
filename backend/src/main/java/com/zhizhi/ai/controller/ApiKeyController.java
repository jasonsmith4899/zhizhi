package com.zhizhi.ai.controller;

import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.Result;
import com.zhizhi.ai.model.dto.ApiKeyDTO;
import com.zhizhi.ai.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final AuthService authService;
    private final AuthUtil authUtil;

    /**
     * 获取当前用户的所有 API Key
     */
    @GetMapping
    public Result<List<ApiKeyDTO>> list(Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        return Result.ok(authService.listApiKeys(userId));
    }

    /**
     * 创建新的 API Key
     */
    @PostMapping
    public Result<ApiKeyDTO> create(@RequestBody Map<String, Object> request,
                                     Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        String name = (String) request.getOrDefault("name", "默认Key");
        String description = (String) request.get("description");
        String assistantPersona = (String) request.get("assistantPersona");
        String merchantBackground = (String) request.get("merchantBackground");
        String answerRules = (String) request.get("answerRules");

        @SuppressWarnings("unchecked")
        List<Number> kbIds = (List<Number>) request.get("knowledgeBaseIds");
        Set<Long> knowledgeBaseIds = null;
        if (kbIds != null) {
            knowledgeBaseIds = new java.util.HashSet<>();
            for (Number n : kbIds) {
                knowledgeBaseIds.add(n.longValue());
            }
        }

        // 创建时返回完整 key 值（仅此一次可见）
        ApiKeyDTO dto = authService.createApiKey(userId, name, description,
                assistantPersona, merchantBackground, answerRules, knowledgeBaseIds);
        return Result.ok(dto);
    }

    /**
     * 更新 API Key（名称、描述、配置、关联知识库）
     */
    @PutMapping("/{id}")
    public Result<ApiKeyDTO> update(@PathVariable Long id,
                                     @RequestBody Map<String, Object> request,
                                     Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String assistantPersona = (String) request.get("assistantPersona");
        String merchantBackground = (String) request.get("merchantBackground");
        String answerRules = (String) request.get("answerRules");

        @SuppressWarnings("unchecked")
        List<Number> kbIds = (List<Number>) request.get("knowledgeBaseIds");
        Set<Long> knowledgeBaseIds = null;
        if (kbIds != null) {
            knowledgeBaseIds = new java.util.HashSet<>();
            for (Number n : kbIds) {
                knowledgeBaseIds.add(n.longValue());
            }
        }

        return Result.ok(authService.updateApiKey(userId, id, name, description,
                assistantPersona, merchantBackground, answerRules, knowledgeBaseIds));
    }

    /**
     * 删除 API Key
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        authService.deleteApiKey(userId, id);
        return Result.ok();
    }
}

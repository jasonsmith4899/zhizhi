package com.zhizhi.ai.controller;

import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.Result;
import com.zhizhi.ai.model.dto.KnowledgeBaseDTO;
import com.zhizhi.ai.model.dto.KnowledgeBaseRequest;
import com.zhizhi.ai.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final AuthUtil authUtil;

    @PostMapping
    public Result<KnowledgeBaseDTO> create(@Valid @RequestBody KnowledgeBaseRequest request,
                                         Authentication authentication) {
        return Result.ok(KnowledgeBaseDTO.fromEntity(
                knowledgeService.create(request, authUtil.getUserId(authentication))));
    }

    @GetMapping
    public Result<List<KnowledgeBaseDTO>> list(Authentication authentication) {
        List<KnowledgeBaseDTO> dtos = knowledgeService.listByUser(authUtil.getUserId(authentication))
                .stream()
                .map(KnowledgeBaseDTO::fromEntity)
                .toList();
        return Result.ok(dtos);
    }

    @GetMapping("/{id}")
    public Result<KnowledgeBaseDTO> getById(@PathVariable Long id, Authentication authentication) {
        return Result.ok(KnowledgeBaseDTO.fromEntity(
                knowledgeService.getById(id, authUtil.getUserId(authentication))));
    }

    @PutMapping("/{id}")
    public Result<KnowledgeBaseDTO> update(@PathVariable Long id,
                                         @Valid @RequestBody KnowledgeBaseRequest request,
                                         Authentication authentication) {
        return Result.ok(KnowledgeBaseDTO.fromEntity(
                knowledgeService.update(id, request, authUtil.getUserId(authentication))));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        knowledgeService.delete(id, authUtil.getUserId(authentication));
        return Result.ok();
    }
}

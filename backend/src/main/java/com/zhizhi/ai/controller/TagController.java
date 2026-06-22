package com.zhizhi.ai.controller;

import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.Result;
import com.zhizhi.ai.model.dto.TagRequest;
import com.zhizhi.ai.model.entity.Tag;
import com.zhizhi.ai.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final AuthUtil authUtil;

    @PostMapping
    public Result<Tag> create(@Valid @RequestBody TagRequest req, Authentication auth) {
        Long userId = authUtil.getUserId(auth);
        return Result.ok(tagService.create(req.getKnowledgeBaseId(), req.getName(), req.getColor(), userId));
    }

    @GetMapping
    public Result<List<Tag>> list(@RequestParam Long knowledgeBaseId, Authentication auth) {
        return Result.ok(tagService.list(knowledgeBaseId, authUtil.getUserId(auth)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        tagService.delete(id, authUtil.getUserId(auth));
        return Result.ok();
    }
}

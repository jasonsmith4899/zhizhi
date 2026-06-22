package com.zhizhi.ai.controller;

import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.Result;
import com.zhizhi.ai.model.dto.CategoryRequest;
import com.zhizhi.ai.model.entity.Category;
import com.zhizhi.ai.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final AuthUtil authUtil;

    @PostMapping
    public Result<Category> create(@Valid @RequestBody CategoryRequest req, Authentication auth) {
        Long userId = authUtil.getUserId(auth);
        return Result.ok(categoryService.create(
                req.getKnowledgeBaseId(), req.getParentId(), req.getName(), req.getSortOrder(), userId));
    }

    @GetMapping
    public Result<List<Category>> list(@RequestParam Long knowledgeBaseId, Authentication auth) {
        return Result.ok(categoryService.list(knowledgeBaseId, authUtil.getUserId(auth)));
    }

    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req, Authentication auth) {
        Long userId = authUtil.getUserId(auth);
        return Result.ok(categoryService.update(id, req.getName(), req.getParentId(), req.getSortOrder(), userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        categoryService.delete(id, authUtil.getUserId(auth));
        return Result.ok();
    }
}

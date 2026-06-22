package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.entity.Category;
import com.zhizhi.ai.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分类（树形目录）管理
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final KnowledgeService knowledgeService;
    private final JdbcTemplate jdbcTemplate;

    public Category create(Long knowledgeBaseId, Long parentId, String name, Integer sortOrder, Long userId) {
        knowledgeService.getById(knowledgeBaseId, userId);  // 校验知识库归属
        Long tenantId = TenantContext.getTenantId();
        if (parentId != null) {
            validateCategory(parentId, knowledgeBaseId);
        }
        return categoryRepository.save(Category.builder()
                .tenantId(tenantId)
                .knowledgeBaseId(knowledgeBaseId)
                .parentId(parentId)
                .name(name)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build());
    }

    public List<Category> list(Long knowledgeBaseId, Long userId) {
        knowledgeService.getById(knowledgeBaseId, userId);
        Long tenantId = TenantContext.getTenantId();
        return categoryRepository.findByTenantIdAndKnowledgeBaseIdOrderBySortOrderAscIdAsc(tenantId, knowledgeBaseId);
    }

    public Category update(Long id, String name, Long parentId, Integer sortOrder, Long userId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("分类"));
        knowledgeService.getById(category.getKnowledgeBaseId(), userId);
        if (parentId != null) {
            if (parentId.equals(id)) {
                throw BusinessException.badRequest("分类不能以自身作为父级");
            }
            validateCategory(parentId, category.getKnowledgeBaseId());
        }
        if (name != null) category.setName(name);
        category.setParentId(parentId);
        if (sortOrder != null) category.setSortOrder(sortOrder);
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("分类"));
        knowledgeService.getById(category.getKnowledgeBaseId(), userId);
        if (categoryRepository.countByParentId(id) > 0) {
            throw BusinessException.badRequest("请先删除子分类");
        }
        // 解除该分类下文档的归属
        jdbcTemplate.update("UPDATE documents SET category_id = NULL WHERE category_id = ?", id);
        categoryRepository.delete(category);
    }

    private void validateCategory(Long categoryId, Long knowledgeBaseId) {
        Category c = categoryRepository.findById(categoryId)
                .orElseThrow(() -> BusinessException.notFound("分类"));
        if (!c.getKnowledgeBaseId().equals(knowledgeBaseId)) {
            throw BusinessException.badRequest("分类不属于该知识库");
        }
    }
}

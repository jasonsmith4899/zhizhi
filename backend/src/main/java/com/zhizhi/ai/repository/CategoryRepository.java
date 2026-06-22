package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByTenantIdAndKnowledgeBaseIdOrderBySortOrderAscIdAsc(Long tenantId, Long knowledgeBaseId);
    long countByParentId(Long parentId);
}

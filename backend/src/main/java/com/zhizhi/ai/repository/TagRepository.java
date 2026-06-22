package com.zhizhi.ai.repository;

import com.zhizhi.ai.model.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByTenantIdAndKnowledgeBaseIdOrderByName(Long tenantId, Long knowledgeBaseId);
    Optional<Tag> findByTenantIdAndKnowledgeBaseIdAndName(Long tenantId, Long knowledgeBaseId, String name);
}

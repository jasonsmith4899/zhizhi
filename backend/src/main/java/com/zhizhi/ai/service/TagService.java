package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.entity.DocumentTag;
import com.zhizhi.ai.model.entity.Tag;
import com.zhizhi.ai.repository.DocumentTagRepository;
import com.zhizhi.ai.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 标签管理 + 文档标签关联
 */
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final DocumentTagRepository documentTagRepository;
    private final KnowledgeService knowledgeService;

    public Tag create(Long knowledgeBaseId, String name, String color, Long userId) {
        knowledgeService.getById(knowledgeBaseId, userId);
        Long tenantId = TenantContext.getTenantId();
        return tagRepository.findByTenantIdAndKnowledgeBaseIdAndName(tenantId, knowledgeBaseId, name)
                .orElseGet(() -> tagRepository.save(Tag.builder()
                        .tenantId(tenantId)
                        .knowledgeBaseId(knowledgeBaseId)
                        .name(name)
                        .color(color)
                        .build()));
    }

    public List<Tag> list(Long knowledgeBaseId, Long userId) {
        knowledgeService.getById(knowledgeBaseId, userId);
        Long tenantId = TenantContext.getTenantId();
        return tagRepository.findByTenantIdAndKnowledgeBaseIdOrderByName(tenantId, knowledgeBaseId);
    }

    @Transactional
    public void delete(Long tagId, Long userId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> BusinessException.notFound("标签"));
        knowledgeService.getById(tag.getKnowledgeBaseId(), userId);
        documentTagRepository.deleteByTagId(tagId);
        tagRepository.delete(tag);
    }

    /** 设置文档的标签（全量覆盖） */
    @Transactional
    public void setDocumentTags(Long documentId, List<Long> tagIds, Long tenantId) {
        documentTagRepository.deleteByDocumentId(documentId);
        if (tagIds == null || tagIds.isEmpty()) return;
        for (Long tagId : tagIds.stream().distinct().toList()) {
            tagRepository.findById(tagId).ifPresent(tag -> {
                if (tenantId != null && !Objects.equals(tag.getTenantId(), tenantId)) {
                    throw BusinessException.forbidden("无权使用此标签");
                }
                documentTagRepository.save(DocumentTag.builder()
                        .documentId(documentId)
                        .tagId(tagId)
                        .tenantId(tag.getTenantId())
                        .build());
            });
        }
    }

    public List<Long> getDocumentTagIds(Long documentId) {
        return documentTagRepository.findTagIdsByDocumentId(documentId);
    }
}

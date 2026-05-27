package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.dto.KnowledgeBaseRequest;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import com.zhizhi.ai.repository.DocumentRepository;
import com.zhizhi.ai.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建知识库
     */
    public KnowledgeBase create(KnowledgeBaseRequest request, Long userId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw BusinessException.badRequest("缺少租户信息");
        }
        KnowledgeBase kb = KnowledgeBase.builder()
                .userId(userId)
                .tenantId(tenantId)
                .name(request.getName())
                .description(request.getDescription())
                .systemPrompt(request.getSystemPrompt())
                .build();
        return knowledgeBaseRepository.save(kb);
    }

    /**
     * 获取知识库详情
     */
    public KnowledgeBase getById(Long id, Long userId) {
        Long tenantId = TenantContext.getTenantId();
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("知识库"));
        if (tenantId != null && !kb.getTenantId().equals(tenantId)) {
            throw BusinessException.forbidden("无权访问此知识库");
        }
        if (!kb.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权访问此知识库");
        }
        return kb;
    }

    /**
     * 获取用户所有知识库
     */
    public List<KnowledgeBase> listByUser(Long userId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return knowledgeBaseRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId);
        }
        return knowledgeBaseRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    /**
     * 更新知识库
     */
    public KnowledgeBase update(Long id, KnowledgeBaseRequest request, Long userId) {
        KnowledgeBase kb = getById(id, userId);
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        if (request.getSystemPrompt() != null) {
            kb.setSystemPrompt(request.getSystemPrompt());
        }
        return knowledgeBaseRepository.save(kb);
    }

    /**
     * 删除知识库（级联清理关联数据）
     */
    @Transactional
    public void delete(Long id, Long userId) {
        KnowledgeBase kb = getById(id, userId);
        Long tenantId = kb.getTenantId();

        // 1. 删除关联对话的消息
        int messagesDeleted = jdbcTemplate.update(
            "DELETE FROM messages WHERE tenant_id = ? AND conversation_id IN (SELECT id FROM conversations WHERE knowledge_base_id = ?)", tenantId, id);
        log.info("删除知识库 {} 关联消息 {} 条", id, messagesDeleted);

        // 2. 删除关联对话的聊天记忆
        jdbcTemplate.update(
            "DELETE FROM spring_ai_chat_memory WHERE conversation_id IN (SELECT CAST(id AS VARCHAR) FROM conversations WHERE knowledge_base_id = ? AND tenant_id = ?)", id, tenantId);

        // 3. 删除关联对话
        int convsDeleted = jdbcTemplate.update("DELETE FROM conversations WHERE knowledge_base_id = ? AND tenant_id = ?", id, tenantId);
        log.info("删除知识库 {} 关联对话 {} 条", id, convsDeleted);

        // 4. 删除向量（同时按knowledge_base_id和tenant_id过滤，防止误删其他租户数据）
        int vectorsDeleted = jdbcTemplate.update(
            "DELETE FROM document_chunks WHERE metadata->>'knowledge_base_id' = ? AND metadata->>'tenant_id' = ?",
            id.toString(), tenantId.toString());
        log.info("删除知识库 {} 关联向量 {} 条", id, vectorsDeleted);

        // 5. 删除文档切片
        jdbcTemplate.update("DELETE FROM document_chunks WHERE knowledge_base_id = ? AND tenant_id = ?", id, tenantId);

        // 6. 删除文档
        int docsDeleted = jdbcTemplate.update("DELETE FROM documents WHERE knowledge_base_id = ? AND tenant_id = ?", id, tenantId);
        log.info("删除知识库 {} 关联文档 {} 个", id, docsDeleted);

        // 7. 删除知识库
        knowledgeBaseRepository.delete(kb);
        log.info("知识库 {} 已删除", id);
    }
}

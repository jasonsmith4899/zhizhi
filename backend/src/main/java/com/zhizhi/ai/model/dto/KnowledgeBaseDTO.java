package com.zhizhi.ai.model.dto;

import com.zhizhi.ai.model.entity.KnowledgeBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeBaseDTO {

    private Long id;
    private String name;
    private String description;
    private String systemPrompt;
    private String status;
    private Integer documentCount;
    private Integer chunkCount;
    private LocalDateTime createdAt;

    public static KnowledgeBaseDTO fromEntity(KnowledgeBase knowledgeBase) {
        return KnowledgeBaseDTO.builder()
                .id(knowledgeBase.getId())
                .name(knowledgeBase.getName())
                .description(knowledgeBase.getDescription())
                .systemPrompt(knowledgeBase.getSystemPrompt())
                .status(knowledgeBase.getStatus())
                .documentCount(knowledgeBase.getDocumentCount())
                .chunkCount(knowledgeBase.getChunkCount())
                .createdAt(knowledgeBase.getCreatedAt())
                .build();
    }
}

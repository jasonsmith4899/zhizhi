package com.zhizhi.ai.model.dto;

import com.zhizhi.ai.model.entity.ApiKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyDTO {

    private Long id;
    private String name;
    private String description;
    private String keyValue;
    private String assistantPersona;
    private String merchantBackground;
    private String answerRules;
    private LocalDateTime createdAt;
    private Set<Long> knowledgeBaseIds;

    public static ApiKeyDTO fromEntity(ApiKey apiKey) {
        return ApiKeyDTO.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .description(apiKey.getDescription())
                .keyValue(apiKey.getKeyValue())
                .assistantPersona(apiKey.getAssistantPersona())
                .merchantBackground(apiKey.getMerchantBackground())
                .answerRules(apiKey.getAnswerRules())
                .createdAt(apiKey.getCreatedAt())
                .knowledgeBaseIds(apiKey.getKnowledgeBaseIds())
                .build();
    }

    /**
     * 返回给前端时隐藏 key 的中间部分
     */
    public static ApiKeyDTO fromEntityMasked(ApiKey apiKey) {
        ApiKeyDTO dto = fromEntity(apiKey);
        if (dto.getKeyValue() != null && dto.getKeyValue().length() > 8) {
            dto.setKeyValue(dto.getKeyValue().substring(0, 4) + "****" + dto.getKeyValue().substring(dto.getKeyValue().length() - 4));
        }
        return dto;
    }
}

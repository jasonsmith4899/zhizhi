package com.zhizhi.ai.model.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhizhi.ai.model.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {

    private Long id;
    private String role;
    private String content;
    private LocalDateTime createdAt;
    private List<ChatResponse.SourceReference> sourceReferences;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static MessageDTO fromEntity(Message message) {
        List<ChatResponse.SourceReference> sources = parseSourceDocuments(message.getSourceDocuments());
        return MessageDTO.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .sourceReferences(sources)
                .build();
    }

    private static List<ChatResponse.SourceReference> parseSourceDocuments(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}

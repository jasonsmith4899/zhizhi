package com.zhizhi.ai.model.dto;

import com.zhizhi.ai.model.entity.Conversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDTO {

    private Long id;
    private String title;
    private String sessionId;
    private String channel;
    private Integer messageCount;
    private LocalDateTime createdAt;

    public static ConversationDTO fromEntity(Conversation conversation) {
        return ConversationDTO.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .sessionId(conversation.getSessionId())
                .channel(conversation.getChannel())
                .messageCount(conversation.getMessageCount())
                .createdAt(conversation.getCreatedAt())
                .build();
    }
}

package com.zhizhi.ai.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "消息不能为空")
    @Size(max = 5000, message = "消息长度不能超过5000字")
    private String message;

    /** 知识库ID（可选，指定后启用RAG） */
    private Long knowledgeBaseId;

    /** 会话ID（可选，传入则继续多轮对话） */
    private String sessionId;
}

package com.zhizhi.ai.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TagRequest {
    private Long knowledgeBaseId;

    @NotBlank(message = "标签名称不能为空")
    private String name;

    private String color;
}

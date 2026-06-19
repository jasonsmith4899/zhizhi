package com.zhizhi.ai.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeBaseRequest {

    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 200, message = "名称长度不能超过200字")
    private String name;

    @Size(max = 1000, message = "描述长度不能超过1000字")
    private String description;
}

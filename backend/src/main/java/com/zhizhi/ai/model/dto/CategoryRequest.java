package com.zhizhi.ai.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {
    private Long knowledgeBaseId;
    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private Integer sortOrder;
}

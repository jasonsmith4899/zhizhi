package com.zhizhi.ai.model.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识图谱关系 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KgRelationDTO {

    private Long id;
    private String sourceName;
    private String targetName;
    private String predicate;
    private Float confidence;
    private Long documentId;
    private String documentName;
    private LocalDateTime createdAt;
}

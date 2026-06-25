package com.zhizhi.ai.model.dto.kg;

import com.zhizhi.ai.model.entity.KgEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识图谱实体 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KgEntityDTO {

    private Long id;
    private String name;
    private String type;
    private String description;
    private int mentionCount;
    private LocalDateTime createdAt;

    public static KgEntityDTO fromEntity(KgEntity entity) {
        KgEntityDTO dto = new KgEntityDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setDescription(entity.getDescription());
        dto.setMentionCount(entity.getMentionCount());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}

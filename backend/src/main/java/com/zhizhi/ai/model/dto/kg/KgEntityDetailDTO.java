package com.zhizhi.ai.model.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识图谱实体详情 DTO（含关联关系）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KgEntityDetailDTO extends KgEntityDTO {

    private List<RelationInfo> relations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationInfo {
        private Long id;
        private String predicate;
        /** direction: "out" = 该实体是source, "in" = 该实体是target */
        private String direction;
        private String otherEntity;
        private Float confidence;
    }
}

package com.zhizhi.ai.model.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识图谱统计数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KgStatsDTO {

    private long entityCount;
    private long relationCount;
    private int avgMentionCount;
    private List<TopEntity> topEntities;
    private List<TypeCount> typeDistribution;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopEntity {
        private String name;
        private String type;
        private int mentionCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeCount {
        private String type;
        private long count;
    }
}

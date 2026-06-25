package com.zhizhi.ai.model.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识图谱子图数据（用于前端可视化）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KgGraphDTO {

    private List<GraphNode> nodes;
    private List<GraphEdge> edges;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphNode {
        private Long id;
        private String name;
        private String type;
        private int mentionCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphEdge {
        private Long source;
        private Long target;
        private String predicate;
        private Float confidence;
    }
}

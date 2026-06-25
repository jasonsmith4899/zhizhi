package com.zhizhi.ai.model.dto.kg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KgGraphDTO 单元测试")
class KgGraphDTOTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        KgGraphDTO dto = new KgGraphDTO();
        List<KgGraphDTO.GraphNode> nodes = List.of(
                new KgGraphDTO.GraphNode(1L, "A", "类型", 5)
        );
        List<KgGraphDTO.GraphEdge> edges = List.of(
                new KgGraphDTO.GraphEdge(1L, 2L, "关系", 0.9f)
        );
        dto.setNodes(nodes);
        dto.setEdges(edges);

        assertEquals(1, dto.getNodes().size());
        assertEquals(1, dto.getEdges().size());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        List<KgGraphDTO.GraphNode> nodes = List.of();
        List<KgGraphDTO.GraphEdge> edges = List.of();
        KgGraphDTO dto = new KgGraphDTO(nodes, edges);

        assertNotNull(dto.getNodes());
        assertNotNull(dto.getEdges());
    }

    @Test
    @DisplayName("GraphNode Getter/Setter")
    void graphNodeGetterSetter() {
        KgGraphDTO.GraphNode node = new KgGraphDTO.GraphNode();
        node.setId(1L);
        node.setName("节点");
        node.setType("人物");
        node.setMentionCount(3);

        assertEquals(1L, node.getId());
        assertEquals("节点", node.getName());
        assertEquals("人物", node.getType());
        assertEquals(3, node.getMentionCount());
    }

    @Test
    @DisplayName("GraphNode AllArgsConstructor")
    void graphNodeAllArgsConstructor() {
        KgGraphDTO.GraphNode node = new KgGraphDTO.GraphNode(1L, "A", "T", 5);

        assertEquals(1L, node.getId());
        assertEquals("A", node.getName());
        assertEquals("T", node.getType());
        assertEquals(5, node.getMentionCount());
    }

    @Test
    @DisplayName("GraphEdge Getter/Setter")
    void graphEdgeGetterSetter() {
        KgGraphDTO.GraphEdge edge = new KgGraphDTO.GraphEdge();
        edge.setSource(1L);
        edge.setTarget(2L);
        edge.setPredicate("属于");
        edge.setConfidence(0.85f);

        assertEquals(1L, edge.getSource());
        assertEquals(2L, edge.getTarget());
        assertEquals("属于", edge.getPredicate());
        assertEquals(0.85f, edge.getConfidence());
    }

    @Test
    @DisplayName("GraphEdge AllArgsConstructor")
    void graphEdgeAllArgsConstructor() {
        KgGraphDTO.GraphEdge edge = new KgGraphDTO.GraphEdge(1L, 2L, "p", 0.5f);

        assertEquals(1L, edge.getSource());
        assertEquals(2L, edge.getTarget());
        assertEquals("p", edge.getPredicate());
        assertEquals(0.5f, edge.getConfidence());
    }

    @Test
    @DisplayName("GraphNode equals/hashCode")
    void graphNodeEqualsHashCode() {
        KgGraphDTO.GraphNode n1 = new KgGraphDTO.GraphNode(1L, "A", "T", 5);
        KgGraphDTO.GraphNode n2 = new KgGraphDTO.GraphNode(1L, "A", "T", 5);
        KgGraphDTO.GraphNode n3 = new KgGraphDTO.GraphNode(2L, "B", "T", 3);

        assertEquals(n1, n2);
        assertNotEquals(n1, n3);
    }

    @Test
    @DisplayName("GraphEdge equals/hashCode")
    void graphEdgeEqualsHashCode() {
        KgGraphDTO.GraphEdge e1 = new KgGraphDTO.GraphEdge(1L, 2L, "p", 0.5f);
        KgGraphDTO.GraphEdge e2 = new KgGraphDTO.GraphEdge(1L, 2L, "p", 0.5f);
        KgGraphDTO.GraphEdge e3 = new KgGraphDTO.GraphEdge(1L, 3L, "p", 0.5f);

        assertEquals(e1, e2);
        assertNotEquals(e1, e3);
    }
}

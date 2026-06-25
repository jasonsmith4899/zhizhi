package com.zhizhi.ai.controller;

import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.GlobalExceptionHandler;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.config.SecurityConfig;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.entity.KgEntity;
import com.zhizhi.ai.model.entity.KgRelation;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import com.zhizhi.ai.repository.ApiKeyRepository;
import com.zhizhi.ai.repository.DocumentRepository;
import com.zhizhi.ai.repository.KgEntityRepository;
import com.zhizhi.ai.repository.KgRelationRepository;
import com.zhizhi.ai.repository.KnowledgeBaseRepository;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KgController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@ActiveProfiles("test")
class KgControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KgEntityRepository entityRepository;

    @MockitoBean
    private KgRelationRepository relationRepository;

    @MockitoBean
    private DocumentRepository documentRepository;

    @MockitoBean
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @MockitoBean
    private AuthUtil authUtil;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private ApiKeyRepository apiKeyRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TenantMemberRepository tenantMemberRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long KB_ID = 10L;

    @BeforeEach
    void setUp() {
        when(authUtil.getUserId(any(Authentication.class))).thenReturn(1L);
        TenantContext.setTenantId(TENANT_ID);

        KnowledgeBase kb = KnowledgeBase.builder()
                .id(KB_ID).tenantId(TENANT_ID).name("Test KB").build();
        when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(kb));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private KgEntity buildEntity(Long id, String name, String type, int mentions) {
        return KgEntity.builder()
                .id(id).tenantId(TENANT_ID).knowledgeBaseId(KB_ID)
                .name(name).type(type).normName(name.toLowerCase())
                .mentionCount(mentions)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private KgRelation buildRelation(Long id, Long sourceId, Long targetId, String predicate) {
        return KgRelation.builder()
                .id(id).tenantId(TENANT_ID).knowledgeBaseId(KB_ID)
                .sourceId(sourceId).targetId(targetId)
                .predicate(predicate).confidence(0.9f)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ========== stats ==========

    @Test
    @WithMockUser
    void stats_success() throws Exception {
        when(entityRepository.countByTenantIdAndKnowledgeBaseId(TENANT_ID, KB_ID)).thenReturn(5L);
        when(relationRepository.countByTenantIdAndKnowledgeBaseId(TENANT_ID, KB_ID)).thenReturn(3L);
        when(entityRepository.sumMentionCountByTenantIdAndKnowledgeBaseId(TENANT_ID, KB_ID)).thenReturn(25L);
        when(entityRepository.findTop20ByTenantIdAndKnowledgeBaseIdOrderByMentionCountDesc(TENANT_ID, KB_ID))
                .thenReturn(List.of(buildEntity(1L, "AI", "概念", 10)));
        when(entityRepository.countByType(TENANT_ID, KB_ID))
                .thenReturn(java.util.Collections.<Object[]>singletonList(new Object[]{"概念", 3L}));

        mockMvc.perform(get("/api/v1/knowledge-bases/{kbId}/kg/stats", KB_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.entityCount").value(5))
                .andExpect(jsonPath("$.data.relationCount").value(3))
                .andExpect(jsonPath("$.data.avgMentionCount").value(5))
                .andExpect(jsonPath("$.data.topEntities").isArray())
                .andExpect(jsonPath("$.data.topEntities[0].name").value("AI"))
                .andExpect(jsonPath("$.data.typeDistribution[0].type").value("概念"));
    }

    @Test
    @WithMockUser
    void stats_kbNotFound_returns404() throws Exception {
        when(knowledgeBaseRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/knowledge-bases/{kbId}/kg/stats", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser
    void stats_wrongTenant_returns403() throws Exception {
        KnowledgeBase otherKb = KnowledgeBase.builder()
                .id(KB_ID).tenantId(999L).name("Other KB").build();
        when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(otherKb));

        mockMvc.perform(get("/api/v1/knowledge-bases/{kbId}/kg/stats", KB_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser
    void stats_noTenantContext_returns401() throws Exception {
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/knowledge-bases/{kbId}/kg/stats", KB_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ========== entities ==========

    @Test
    @WithMockUser
    void entities_success() throws Exception {
        Page<KgEntity> page = new PageImpl<>(
                List.of(buildEntity(1L, "AI", "概念", 10)),
                PageRequest.of(0, 20), 1);
        when(entityRepository.searchEntities(eq(TENANT_ID), eq(KB_ID),
                isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/knowledge-bases/{kbId}/kg/entities", KB_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].name").value("AI"))
                .andExpect(jsonPath("$.data.content[0].type").value("概念"));
    }

    @Test
    @WithMockUser
    void entities_withSearchAndType_filtersCorrectly() throws Exception {
        Page<KgEntity> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(entityRepository.searchEntities(eq(TENANT_ID), eq(KB_ID),
                eq("test"), eq("人物"), any(PageRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/knowledge-bases/{kbId}/kg/entities", KB_ID)
                        .param("search", "test")
                        .param("type", "人物"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ========== relations ==========

    @Test
    @WithMockUser
    void relations_success() throws Exception {
        KgRelation rel = buildRelation(1L, 1L, 2L, "属于");
        Page<KgRelation> page = new PageImpl<>(List.of(rel), PageRequest.of(0, 20), 1);

        when(relationRepository.searchRelations(eq(TENANT_ID), eq(KB_ID),
                isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);
        when(entityRepository.findAllById(any())).thenReturn(
                List.of(buildEntity(1L, "A", "概念", 5), buildEntity(2L, "B", "人物", 3)));
        when(documentRepository.findAllById(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/knowledge-bases/{kbId}/kg/relations", KB_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].predicate").value("属于"))
                .andExpect(jsonPath("$.data.content[0].sourceName").value("A"))
                .andExpect(jsonPath("$.data.content[0].targetName").value("B"));
    }

    // ========== entityDetail ==========

    @Test
    @WithMockUser
    void entityDetail_success() throws Exception {
        KgEntity entity = buildEntity(1L, "AI", "概念", 10);
        entity.setDescription("人工智能");
        when(entityRepository.findById(1L)).thenReturn(Optional.of(entity));

        KgRelation rel = buildRelation(1L, 1L, 2L, "包含");
        when(relationRepository.findByEntityId(eq(TENANT_ID), eq(KB_ID), eq(1L), any(PageRequest.class)))
                .thenReturn(List.of(rel));
        when(entityRepository.findAllById(any())).thenReturn(
                List.of(entity, buildEntity(2L, "ML", "概念", 5)));

        mockMvc.perform(get("/api/v1/knowledge-bases/{kbId}/kg/entities/{entityId}", KB_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("AI"))
                .andExpect(jsonPath("$.data.description").value("人工智能"))
                .andExpect(jsonPath("$.data.relations").isArray())
                .andExpect(jsonPath("$.data.relations[0].predicate").value("包含"))
                .andExpect(jsonPath("$.data.relations[0].direction").value("out"))
                .andExpect(jsonPath("$.data.relations[0].otherEntity").value("ML"));
    }

    @Test
    @WithMockUser
    void entityDetail_notFound_returns404() throws Exception {
        when(entityRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/knowledge-bases/{kbId}/kg/entities/{entityId}", KB_ID, 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser
    void entityDetail_wrongTenant_returns403() throws Exception {
        KgEntity entity = KgEntity.builder()
                .id(1L).tenantId(999L).knowledgeBaseId(KB_ID)
                .name("AI").type("概念").normName("ai").mentionCount(1)
                .build();
        when(entityRepository.findById(1L)).thenReturn(Optional.of(entity));

        mockMvc.perform(get("/api/v1/knowledge-bases/{kbId}/kg/entities/{entityId}", KB_ID, 1L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    // ========== graph ==========

    @Test
    @WithMockUser
    void graph_withSeedEntity_traversesSubgraph() throws Exception {
        KgEntity seed = buildEntity(1L, "AI", "概念", 10);
        when(entityRepository.findById(1L)).thenReturn(Optional.of(seed));

        KgRelation rel = buildRelation(1L, 1L, 2L, "包含");
        when(relationRepository.findByNodeIds(eq(TENANT_ID), eq(KB_ID), anyList()))
                .thenReturn(List.of(rel));

        KgEntity related = buildEntity(2L, "ML", "概念", 5);
        when(entityRepository.findAllById(any())).thenReturn(List.of(seed, related));

        mockMvc.perform(get("/api/v1/knowledge-bases/{kbId}/kg/graph", KB_ID)
                        .param("seedEntityId", "1")
                        .param("maxHops", "2")
                        .param("maxNodes", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nodes").isArray())
                .andExpect(jsonPath("$.data.edges").isArray());
    }

    @Test
    @WithMockUser
    void graph_withoutSeedEntity_returnsTopNodes() throws Exception {
        List<KgEntity> topEntities = List.of(
                buildEntity(1L, "AI", "概念", 10),
                buildEntity(2L, "ML", "概念", 5));
        when(entityRepository.findTop20ByTenantIdAndKnowledgeBaseIdOrderByMentionCountDesc(TENANT_ID, KB_ID))
                .thenReturn(topEntities);

        KgRelation rel = buildRelation(1L, 1L, 2L, "包含");
        when(relationRepository.findByNodeIds(eq(TENANT_ID), eq(KB_ID), anyList()))
                .thenReturn(List.of(rel));
        when(entityRepository.findAllById(any())).thenReturn(topEntities);

        mockMvc.perform(get("/api/v1/knowledge-bases/{kbId}/kg/graph", KB_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nodes").isArray())
                .andExpect(jsonPath("$.data.edges").isArray());
    }

    // ========== search ==========

    @Test
    @WithMockUser
    void search_success() throws Exception {
        when(entityRepository.searchByName(eq(TENANT_ID), eq(KB_ID), eq("ai"), any(PageRequest.class)))
                .thenReturn(List.of(buildEntity(1L, "AI", "概念", 10)));

        mockMvc.perform(get("/api/v1/knowledge-bases/{kbId}/kg/search", KB_ID)
                        .param("q", "ai")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("AI"));
    }

    @Test
    @WithMockUser
    void search_emptyResult_returnsEmptyArray() throws Exception {
        when(entityRepository.searchByName(eq(TENANT_ID), eq(KB_ID), eq("nonexistent"), any(PageRequest.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/knowledge-bases/{kbId}/kg/search", KB_ID)
                        .param("q", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}

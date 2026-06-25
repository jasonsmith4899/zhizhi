package com.zhizhi.ai.service;

import com.zhizhi.ai.model.entity.KgEntity;
import com.zhizhi.ai.model.entity.KgRelation;
import com.zhizhi.ai.repository.KgEntityRepository;
import com.zhizhi.ai.repository.KgRelationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.lang.reflect.Field;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * KnowledgeGraphService 单元测试。
 *
 * <p>由于 extractAndStore / querySubgraph / querySubgraphStructured 内部构造 ChatClient，
 * 测试需要通过 mockStatic(ChatClient.Builder) 拦截静态 builder 调用。
 * 清理方法（deleteByDocument / deleteByKnowledgeBase）直接对 repository mock 进行验证。
 */
class KnowledgeGraphServiceTest {

    @Mock
    private ChatModel chatModel;
    @Mock
    private KgEntityRepository entityRepository;
    @Mock
    private KgRelationRepository relationRepository;

    @InjectMocks
    private KnowledgeGraphService service;

    private AutoCloseable mocks;

    // ---- 常量 ----
    private static final Long TENANT_ID = 1L;
    private static final Long KB_ID = 10L;
    private static final Long DOC_ID = 100L;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        setField("enabled", true);
        setField("confidenceThreshold", 0.6);
        setField("maxHops", 2);
        setField("maxSubgraphTriples", 30);
        setField("maxExtractChars", 6000);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // ==================== 辅助方法 ====================

    private void setField(String name, Object value) {
        try {
            Field f = KnowledgeGraphService.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(service, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 构建 mock ChatClient 链：builder(chatModel) -> builder.build() -> prompt -> system -> user -> call -> content。
     * 调用方可指定返回的 LLM 文本。
     */
    @SuppressWarnings("unchecked")
    private MockedStatic<ChatClient> mockChatClientStatic(String llmResponse) {
        ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
        ChatClient mockClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockCallResponse = mock(ChatClient.CallResponseSpec.class);

        when(mockBuilder.build()).thenReturn(mockClient);
        when(mockClient.prompt()).thenReturn(mockSpec);
        when(mockSpec.system(anyString())).thenReturn(mockSpec);
        when(mockSpec.user(anyString())).thenReturn(mockSpec);
        when(mockSpec.call()).thenReturn(mockCallResponse);
        when(mockCallResponse.content()).thenReturn(llmResponse);

        MockedStatic<ChatClient> staticMock = mockStatic(ChatClient.class);
        staticMock.when(() -> ChatClient.builder(chatModel)).thenReturn(mockBuilder);
        return staticMock;
    }

    /**
     * 构建一条 KgEntity stub（不持久化）。
     */
    private KgEntity stubEntity(Long id, String name, String type) {
        KgEntity e = new KgEntity();
        e.setId(id);
        e.setTenantId(TENANT_ID);
        e.setKnowledgeBaseId(KB_ID);
        e.setName(name);
        e.setType(type);
        e.setNormName(name.toLowerCase().replaceAll("\\s+", ""));
        e.setMentionCount(1);
        return e;
    }

    // ==================== extractAndStore ====================

    @Nested
    class ExtractAndStoreTests {

        @Test
        void disabledFlag_returnsEarly_noRepoCalls() {
            setField("enabled", false);

            service.extractAndStore(DOC_ID, KB_ID, TENANT_ID, "some content");

            verifyNoInteractions(entityRepository);
            verifyNoInteractions(relationRepository);
        }

        @Test
        void nullContent_returnsEarly() {
            service.extractAndStore(DOC_ID, KB_ID, TENANT_ID, null);

            verifyNoInteractions(entityRepository);
            verifyNoInteractions(relationRepository);
        }

        @Test
        void blankContent_returnsEarly() {
            service.extractAndStore(DOC_ID, KB_ID, TENANT_ID, "   \n  ");

            verifyNoInteractions(entityRepository);
            verifyNoInteractions(relationRepository);
        }

        @Test
        void happyPath_filtersLowConfidenceAndSelfLoop() {
            // LLM 返回 4 条三元组: 1 正常入库, 1 低置信度过滤, 1 自环跳过, 1 正常入库
            String llmJson = """
                    [
                      {"source":"苹果","sourceType":"组织","target":"库克","targetType":"人物","predicate":"由...领导","confidence":0.9},
                      {"source":"微软","sourceType":"组织","target":"纳德拉","targetType":"人物","predicate":"由...领导","confidence":0.3},
                      {"source":"乔布斯","sourceType":"人物","target":"乔布斯","targetType":"人物","predicate":"是","confidence":0.95},
                      {"source":"谷歌","sourceType":"组织","target":"DeepMind","targetType":"组织","predicate":"收购","confidence":0.8}
                    ]
                    """;

            KgEntity apple = stubEntity(1L, "苹果", "组织");
            KgEntity cook = stubEntity(2L, "库克", "人物");
            KgEntity jobs = stubEntity(3L, "乔布斯", "人物");
            KgEntity google = stubEntity(4L, "谷歌", "组织");
            KgEntity deepmind = stubEntity(5L, "DeepMind", "组织");

            when(entityRepository.findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(
                    eq(TENANT_ID), eq(KB_ID), eq("苹果"), eq("组织")))
                    .thenReturn(Optional.of(apple));
            when(entityRepository.findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(
                    eq(TENANT_ID), eq(KB_ID), eq("库克"), eq("人物")))
                    .thenReturn(Optional.of(cook));
            // 自环: source 和 target 归一化名相同 ("乔布斯") → 同一个 entity
            when(entityRepository.findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(
                    eq(TENANT_ID), eq(KB_ID), eq("乔布斯"), eq("人物")))
                    .thenReturn(Optional.of(jobs));
            when(entityRepository.findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(
                    eq(TENANT_ID), eq(KB_ID), eq("谷歌"), eq("组织")))
                    .thenReturn(Optional.of(google));
            when(entityRepository.findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(
                    eq(TENANT_ID), eq(KB_ID), eq("deepmind"), eq("组织")))
                    .thenReturn(Optional.of(deepmind));

            when(entityRepository.save(any(KgEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(relationRepository.save(any(KgRelation.class))).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<ChatClient> ignored = mockChatClientStatic(llmJson)) {
                service.extractAndStore(DOC_ID, KB_ID, TENANT_ID, "苹果CEO库克领导公司。");
            }

            // 只应保存 2 条关系 (苹果→库克, 谷歌→DeepMind); 低置信度和自环被过滤
            verify(relationRepository, times(2)).save(any(KgRelation.class));

            verify(relationRepository).save(argThat(r ->
                    r.getSourceId().equals(1L)
                            && r.getTargetId().equals(2L)
                            && r.getPredicate().equals("由...领导")
                            && r.getDocumentId().equals(DOC_ID)
                            && r.getTenantId().equals(TENANT_ID)
                            && r.getKnowledgeBaseId().equals(KB_ID)));

            verify(relationRepository).save(argThat(r ->
                    r.getSourceId().equals(4L)
                            && r.getTargetId().equals(5L)
                            && r.getPredicate().equals("收购")));
        }

        @Test
        void emptyLlmOutput_noTriplesSaved() {
            when(entityRepository.findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(
                    anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(Optional.of(stubEntity(1L, "X", "未知")));
            when(relationRepository.save(any(KgRelation.class))).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<ChatClient> ignored = mockChatClientStatic("[]")) {
                service.extractAndStore(DOC_ID, KB_ID, TENANT_ID, "some text");
            }

            verify(relationRepository, never()).save(any(KgRelation.class));
        }

        @Test
        void llmReturnsSurroundingText_stillParsesJson() {
            String llmOutput = "好的，这是抽取结果：\n```json\n" +
                    "[{\"source\":\"A\",\"sourceType\":\"概念\",\"target\":\"B\",\"targetType\":\"概念\",\"predicate\":\"包含\",\"confidence\":0.85}]\n" +
                    "```\n以上为抽取结果。";

            KgEntity entityA = stubEntity(10L, "A", "概念");
            KgEntity entityB = stubEntity(20L, "B", "概念");

            when(entityRepository.findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(
                    eq(TENANT_ID), eq(KB_ID), eq("a"), eq("概念")))
                    .thenReturn(Optional.of(entityA));
            when(entityRepository.findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(
                    eq(TENANT_ID), eq(KB_ID), eq("b"), eq("概念")))
                    .thenReturn(Optional.of(entityB));
            when(entityRepository.save(any(KgEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(relationRepository.save(any(KgRelation.class))).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<ChatClient> ignored = mockChatClientStatic(llmOutput)) {
                service.extractAndStore(DOC_ID, KB_ID, TENANT_ID, "A contains B");
            }

            verify(relationRepository, times(1)).save(argThat(r ->
                    r.getSourceId().equals(10L)
                            && r.getTargetId().equals(20L)
                            && r.getPredicate().equals("包含")));
        }

        @Test
        void llmReturnsMalformedJson_noTriplesSaved() {
            when(relationRepository.save(any(KgRelation.class))).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<ChatClient> ignored = mockChatClientStatic("这不是JSON")) {
                service.extractAndStore(DOC_ID, KB_ID, TENANT_ID, "some text");
            }

            verify(relationRepository, never()).save(any(KgRelation.class));
        }

        @Test
        void llmReturnsEmptyArrayBracket_noTriplesSaved() {
            when(relationRepository.save(any(KgRelation.class))).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<ChatClient> ignored = mockChatClientStatic("[ ]")) {
                service.extractAndStore(DOC_ID, KB_ID, TENANT_ID, "text");
            }

            verify(relationRepository, never()).save(any(KgRelation.class));
        }

        @Test
        void newEntityCreated_whenNotExists() {
            String llmJson = """
                    [{"source":"OpenAI","sourceType":"组织","target":"GPT","targetType":"产品","predicate":"开发","confidence":0.95}]
                    """;

            when(entityRepository.findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(
                    eq(TENANT_ID), eq(KB_ID), eq("openai"), eq("组织")))
                    .thenReturn(Optional.empty());
            when(entityRepository.findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(
                    eq(TENANT_ID), eq(KB_ID), eq("gpt"), eq("产品")))
                    .thenReturn(Optional.empty());

            KgEntity newOpenAI = stubEntity(50L, "OpenAI", "组织");
            KgEntity newGPT = stubEntity(51L, "GPT", "产品");
            when(entityRepository.save(any(KgEntity.class)))
                    .thenReturn(newOpenAI)
                    .thenReturn(newGPT);
            when(relationRepository.save(any(KgRelation.class))).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<ChatClient> ignored = mockChatClientStatic(llmJson)) {
                service.extractAndStore(DOC_ID, KB_ID, TENANT_ID, "OpenAI developed GPT");
            }

            // 两个新实体 + 一条关系
            verify(entityRepository, times(2)).save(any(KgEntity.class));
            verify(relationRepository).save(argThat(r ->
                    r.getSourceId().equals(50L)
                            && r.getTargetId().equals(51L)
                            && r.getPredicate().equals("开发")));
        }

        @Test
        void entityMentionCountIncremented_whenExists() {
            String llmJson = """
                    [{"source":"智谱","sourceType":"组织","target":"ChatGLM","targetType":"产品","predicate":"推出","confidence":0.9}]
                    """;

            KgEntity existingZhipu = stubEntity(60L, "智谱", "组织");
            existingZhipu.setMentionCount(3);
            KgEntity existingGLM = stubEntity(61L, "ChatGLM", "产品");
            existingGLM.setMentionCount(5);

            when(entityRepository.findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(
                    eq(TENANT_ID), eq(KB_ID), eq("智谱"), eq("组织")))
                    .thenReturn(Optional.of(existingZhipu));
            when(entityRepository.findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(
                    eq(TENANT_ID), eq(KB_ID), eq("chatglm"), eq("产品")))
                    .thenReturn(Optional.of(existingGLM));
            when(entityRepository.save(any(KgEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(relationRepository.save(any(KgRelation.class))).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<ChatClient> ignored = mockChatClientStatic(llmJson)) {
                service.extractAndStore(DOC_ID, KB_ID, TENANT_ID, "智谱推出ChatGLM");
            }

            // mentionCount 从 3→4, 5→6
            verify(entityRepository).save(argThat(e ->
                    e.getId().equals(60L) && e.getMentionCount() == 4));
            verify(entityRepository).save(argThat(e ->
                    e.getId().equals(61L) && e.getMentionCount() == 6));
        }

        @Test
        void defaultTypeUsed_whenTypeIsBlank() {
            String llmJson = """
                    [{"source":"某个实体","target":"另一个实体","predicate":"相关","confidence":0.8}]
                    """;

            KgEntity e1 = stubEntity(70L, "某个实体", "未知");
            KgEntity e2 = stubEntity(71L, "另一个实体", "未知");

            when(entityRepository.findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(
                    eq(TENANT_ID), eq(KB_ID), eq("某个实体"), eq("未知")))
                    .thenReturn(Optional.of(e1));
            when(entityRepository.findByTenantIdAndKnowledgeBaseIdAndNormNameAndType(
                    eq(TENANT_ID), eq(KB_ID), eq("另一个实体"), eq("未知")))
                    .thenReturn(Optional.of(e2));
            when(entityRepository.save(any(KgEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(relationRepository.save(any(KgRelation.class))).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<ChatClient> ignored = mockChatClientStatic(llmJson)) {
                service.extractAndStore(DOC_ID, KB_ID, TENANT_ID, "text");
            }

            verify(relationRepository).save(argThat(r ->
                    r.getSourceId().equals(70L) && r.getTargetId().equals(71L)));
        }

        @Test
        void llmException_doesNotThrow() {
            ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
            when(mockBuilder.build()).thenThrow(new RuntimeException("LLM unavailable"));

            try (MockedStatic<ChatClient> staticMock = mockStatic(ChatClient.class)) {
                staticMock.when(() -> ChatClient.builder(chatModel)).thenReturn(mockBuilder);

                // 不应抛出异常（catch 块吞掉）
                service.extractAndStore(DOC_ID, KB_ID, TENANT_ID, "some content");
            }

            verifyNoInteractions(relationRepository);
        }
    }

    // ==================== querySubgraph ====================

    @Nested
    class QuerySubgraphTests {

        @Test
        void disabledFlag_returnsNull() {
            setField("enabled", false);

            String result = service.querySubgraph("test query", Set.of(KB_ID), TENANT_ID);

            assertThat(result).isNull();
        }

        @Test
        void nullQuery_returnsNull() {
            String result = service.querySubgraph(null, Set.of(KB_ID), TENANT_ID);

            assertThat(result).isNull();
        }

        @Test
        void blankQuery_returnsNull() {
            String result = service.querySubgraph("   ", Set.of(KB_ID), TENANT_ID);

            assertThat(result).isNull();
        }

        @Test
        void emptyKnowledgeBaseIds_returnsNull() {
            String result = service.querySubgraph("test query", Collections.emptySet(), TENANT_ID);

            assertThat(result).isNull();
        }

        @Test
        void noRecognizedEntities_returnsNull() {
            try (MockedStatic<ChatClient> ignored = mockChatClientStatic("[]")) {
                String result = service.querySubgraph("random question", Set.of(KB_ID), TENANT_ID);

                assertThat(result).isNull();
            }
            verifyNoInteractions(entityRepository);
        }

        @Test
        void noSeedEntitiesFound_returnsNull() {
            when(entityRepository.findByTenantIdAndKnowledgeBaseIdInAndNormNameIn(
                    eq(TENANT_ID), anyList(), anyList()))
                    .thenReturn(Collections.emptyList());

            try (MockedStatic<ChatClient> ignored = mockChatClientStatic("[\"未知实体\"]")) {
                String result = service.querySubgraph("未知实体是什么?", Set.of(KB_ID), TENANT_ID);

                assertThat(result).isNull();
            }
            verifyNoInteractions(relationRepository);
        }

        @Test
        void emptySubgraph_returnsNull() {
            KgEntity seed = stubEntity(1L, "Test", "概念");
            when(entityRepository.findByTenantIdAndKnowledgeBaseIdInAndNormNameIn(
                    eq(TENANT_ID), anyList(), anyList()))
                    .thenReturn(List.of(seed));
            when(relationRepository.traverseSubgraph(anyList(), eq(TENANT_ID), anyList(), eq(2), eq(30)))
                    .thenReturn(Collections.emptyList());

            try (MockedStatic<ChatClient> ignored = mockChatClientStatic("[\"Test\"]")) {
                String result = service.querySubgraph("What is Test?", Set.of(KB_ID), TENANT_ID);

                assertThat(result).isNull();
            }
        }

        @Test
        void happyPath_returnsFormattedSubgraph() {
            KgEntity entityA = stubEntity(1L, "EntityA", "组织");
            when(entityRepository.findByTenantIdAndKnowledgeBaseIdInAndNormNameIn(
                    eq(TENANT_ID), anyList(), anyList()))
                    .thenReturn(List.of(entityA));
            when(relationRepository.traverseSubgraph(anyList(), eq(TENANT_ID), anyList(), eq(2), eq(30)))
                    .thenReturn(List.of(
                            new Object[]{"EntityA", "属于", "GroupB", 0.9},
                            new Object[]{"EntityA", "开发", "ProductC", 0.85}
                    ));

            try (MockedStatic<ChatClient> ignored = mockChatClientStatic("[\"EntityA\"]")) {
                String result = service.querySubgraph("EntityA 的相关信息", Set.of(KB_ID), TENANT_ID);

                assertThat(result).isNotNull();
                assertThat(result).contains("EntityA —属于→ GroupB");
                assertThat(result).contains("EntityA —开发→ ProductC");
            }

            verify(relationRepository).traverseSubgraph(
                    eq(List.of(1L)), eq(TENANT_ID), anyList(), eq(2), eq(30));
        }

        @Test
        void llmEntityRecognitionFails_returnsNull() {
            ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
            when(mockBuilder.build()).thenThrow(new RuntimeException("LLM down"));

            try (MockedStatic<ChatClient> staticMock = mockStatic(ChatClient.class)) {
                staticMock.when(() -> ChatClient.builder(chatModel)).thenReturn(mockBuilder);

                String result = service.querySubgraph("test", Set.of(KB_ID), TENANT_ID);

                assertThat(result).isNull();
            }
            verifyNoInteractions(entityRepository);
        }

        @Test
        void duplicateRows_dedupedInOutput() {
            KgEntity seed = stubEntity(1L, "A", "概念");
            when(entityRepository.findByTenantIdAndKnowledgeBaseIdInAndNormNameIn(
                    eq(TENANT_ID), anyList(), anyList()))
                    .thenReturn(List.of(seed));
            when(relationRepository.traverseSubgraph(anyList(), eq(TENANT_ID), anyList(), eq(2), eq(30)))
                    .thenReturn(List.of(
                            new Object[]{"A", "关联", "B", 0.9},
                            new Object[]{"A", "关联", "B", 0.9}
                    ));

            try (MockedStatic<ChatClient> ignored = mockChatClientStatic("[\"A\"]")) {
                String result = service.querySubgraph("A", Set.of(KB_ID), TENANT_ID);

                assertThat(result).isNotNull();
                // distinct 应去重为一行
                assertThat(result.lines().count()).isEqualTo(1);
            }
        }
    }

    // ==================== querySubgraphStructured ====================

    @Nested
    class QuerySubgraphStructuredTests {

        @Test
        void disabledFlag_returnsNull() {
            setField("enabled", false);

            List<Map<String, Object>> result =
                    service.querySubgraphStructured("query", Set.of(KB_ID), TENANT_ID);

            assertThat(result).isNull();
        }

        @Test
        void nullQuery_returnsNull() {
            assertThat(service.querySubgraphStructured(null, Set.of(KB_ID), TENANT_ID)).isNull();
        }

        @Test
        void emptyKbIds_returnsNull() {
            assertThat(service.querySubgraphStructured("query", Collections.emptySet(), TENANT_ID)).isNull();
        }

        @Test
        void happyPath_returnsStructuredTriples() {
            KgEntity seed = stubEntity(1L, "Alpha", "组织");
            when(entityRepository.findByTenantIdAndKnowledgeBaseIdInAndNormNameIn(
                    eq(TENANT_ID), anyList(), anyList()))
                    .thenReturn(List.of(seed));
            when(relationRepository.traverseSubgraph(anyList(), eq(TENANT_ID), anyList(), eq(2), eq(30)))
                    .thenReturn(List.of(
                            new Object[]{"Alpha", "拥有", "Beta", 0.92},
                            new Object[]{"Alpha", "开发", "Gamma", 0.88}
                    ));

            try (MockedStatic<ChatClient> ignored = mockChatClientStatic("[\"Alpha\"]")) {
                List<Map<String, Object>> result =
                        service.querySubgraphStructured("Alpha info", Set.of(KB_ID), TENANT_ID);

                assertThat(result).hasSize(2);
                assertThat(result.get(0))
                        .containsEntry("sourceName", "Alpha")
                        .containsEntry("predicate", "拥有")
                        .containsEntry("targetName", "Beta");
                assertThat(result.get(1))
                        .containsEntry("sourceName", "Alpha")
                        .containsEntry("predicate", "开发")
                        .containsEntry("targetName", "Gamma");
            }
        }

        @Test
        void noSeeds_returnsNull() {
            when(entityRepository.findByTenantIdAndKnowledgeBaseIdInAndNormNameIn(
                    eq(TENANT_ID), anyList(), anyList()))
                    .thenReturn(Collections.emptyList());

            try (MockedStatic<ChatClient> ignored = mockChatClientStatic("[\"X\"]")) {
                assertThat(service.querySubgraphStructured("X", Set.of(KB_ID), TENANT_ID)).isNull();
            }
        }
    }

    // ==================== deleteByDocument ====================

    @Nested
    class DeleteByDocumentTests {

        @Test
        void happyPath_callsRepository() {
            service.deleteByDocument(DOC_ID);

            verify(relationRepository).deleteByDocumentId(DOC_ID);
        }

        @Test
        void repositoryException_doesNotThrow() {
            doThrow(new RuntimeException("DB error"))
                    .when(relationRepository).deleteByDocumentId(DOC_ID);

            // 捕获异常不应向上传播
            service.deleteByDocument(DOC_ID);

            verify(relationRepository).deleteByDocumentId(DOC_ID);
        }
    }

    // ==================== deleteByKnowledgeBase ====================

    @Nested
    class DeleteByKnowledgeBaseTests {

        @Test
        void withTenantId_deletesBothEntityAndRelation() {
            service.deleteByKnowledgeBase(TENANT_ID, KB_ID);

            verify(relationRepository).deleteByTenantIdAndKnowledgeBaseId(TENANT_ID, KB_ID);
            verify(entityRepository).deleteByTenantIdAndKnowledgeBaseId(TENANT_ID, KB_ID);
        }

        @Test
        void nullTenantId_fallsBackToKbOnlyDelete() {
            service.deleteByKnowledgeBase(null, KB_ID);

            verify(relationRepository).deleteByKnowledgeBaseId(KB_ID);
            verify(entityRepository).deleteByKnowledgeBaseId(KB_ID);
            verify(relationRepository, never()).deleteByTenantIdAndKnowledgeBaseId(anyLong(), anyLong());
            verify(entityRepository, never()).deleteByTenantIdAndKnowledgeBaseId(anyLong(), anyLong());
        }

        @Test
        void repositoryException_doesNotThrow() {
            doThrow(new RuntimeException("constraint violation"))
                    .when(relationRepository).deleteByTenantIdAndKnowledgeBaseId(TENANT_ID, KB_ID);

            service.deleteByKnowledgeBase(TENANT_ID, KB_ID);

            verify(relationRepository).deleteByTenantIdAndKnowledgeBaseId(TENANT_ID, KB_ID);
        }
    }
}

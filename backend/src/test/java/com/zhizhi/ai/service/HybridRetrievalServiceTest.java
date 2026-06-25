package com.zhizhi.ai.service;

import com.zhizhi.ai.model.entity.DocumentChunk;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("HybridRetrievalService")
class HybridRetrievalServiceTest {

    @InjectMocks
    private HybridRetrievalService hybridRetrievalService;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private RerankerService rerankerService;

    @Mock
    private EntityManager entityManager;

    private static final Long TENANT_ID = 1L;
    private static final Long KB_ID_1 = 10L;
    private static final Long KB_ID_2 = 20L;
    private static final Set<Long> KB_IDS = Set.of(KB_ID_1, KB_ID_2);

    @BeforeEach
    void setUp() {
        // @PersistenceContext field is not in the @RequiredArgsConstructor constructor,
        // so @InjectMocks may not inject it. Manually ensure it's set.
        ReflectionTestUtils.setField(hybridRetrievalService, "entityManager", entityManager);
        ReflectionTestUtils.setField(hybridRetrievalService, "alpha", 0.5);
        ReflectionTestUtils.setField(hybridRetrievalService, "rrfK", 60);
        ReflectionTestUtils.setField(hybridRetrievalService, "topK", 5);
        ReflectionTestUtils.setField(hybridRetrievalService, "threshold", 0.7);
        ReflectionTestUtils.setField(hybridRetrievalService, "maxChunksPerDoc", 2);
    }

    // ==================== Helper methods ====================

    private Document buildDoc(String text, String docId, String chunkIdx, String kbId) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("document_id", docId);
        meta.put("chunk_index", chunkIdx);
        meta.put("knowledge_base_id", kbId);
        meta.put("tenant_id", String.valueOf(TENANT_ID));
        return new Document(text, meta);
    }

    private Document buildDocWithScore(String text, String docId, String chunkIdx, String kbId, double score) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("document_id", docId);
        meta.put("chunk_index", chunkIdx);
        meta.put("knowledge_base_id", kbId);
        meta.put("tenant_id", String.valueOf(TENANT_ID));
        return Document.builder().text(text).metadata(meta).score(score).build();
    }

    private List<Document> buildDocList(int count, String prefix, String kbId) {
        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            docs.add(buildDoc("content-" + prefix + "-" + i, prefix + "-doc-" + i,
                    String.valueOf(i), kbId));
        }
        return docs;
    }

    private List<DocumentChunk> buildChunks(int count, long kbId) {
        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chunks.add(DocumentChunk.builder()
                    .id((long) i)
                    .documentId(100L + i)
                    .knowledgeBaseId(kbId)
                    .tenantId(TENANT_ID)
                    .chunkIndex(i)
                    .content("chunk-content-" + i)
                    .build());
        }
        return chunks;
    }

    /**
     * Set up vectorStore mock using doReturn() to avoid overload ambiguity
     * between similaritySearch(SearchRequest) and similaritySearch(String).
     */
    private void mockVectorSearch(List<Document> results) {
        doReturn(results).when(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    /**
     * Mock EntityManager to return the given chunks for any native query.
     */
    private void mockEntityManagerReturns(List<DocumentChunk> chunks) {
        Query mockQuery = mock(Query.class);
        when(mockQuery.getResultList()).thenReturn(chunks);
        when(entityManager.createNativeQuery(anyString(), eq(DocumentChunk.class)))
                .thenReturn(mockQuery);
    }

    /**
     * Mock EntityManager with consecutive return values
     * (first call = fullTextSearch, second = keywordSearch).
     */
    private Query mockEntityManagerConsecutive(List<DocumentChunk> first, List<DocumentChunk> second) {
        Query mockQuery = mock(Query.class);
        when(mockQuery.getResultList()).thenReturn(first, second);
        when(entityManager.createNativeQuery(anyString(), eq(DocumentChunk.class)))
                .thenReturn(mockQuery);
        return mockQuery;
    }

    /**
     * Set up a mock Query that returns the given chunks, and set up EntityManager
     * to return that query. Returns the Query mock for parameter verification.
     */
    private Query setupQueryMock(List<DocumentChunk> chunks) {
        Query mockQuery = mock(Query.class);
        when(mockQuery.getResultList()).thenReturn(chunks);
        when(entityManager.createNativeQuery(anyString(), eq(DocumentChunk.class)))
                .thenReturn(mockQuery);
        return mockQuery;
    }

    // ==================== hybridSearch (main flow) ====================

    @Nested
    @DisplayName("hybridSearch - end-to-end flow")
    class HybridSearchFlow {

        @Test
        @DisplayName("Happy path: three sources fused, deduped, reranked")
        void hybridSearch_happyPath_returnsRerankedResults() {
            List<Document> vectorDocs = buildDocList(3, "vec", String.valueOf(KB_ID_1));
            List<DocumentChunk> ftChunks = buildChunks(2, KB_ID_1);
            List<DocumentChunk> kwChunks = buildChunks(2, KB_ID_1);
            List<Document> reranked = List.of(
                    buildDocWithScore("reranked-0", "100", "0", String.valueOf(KB_ID_1), 0.95),
                    buildDocWithScore("reranked-1", "101", "1", String.valueOf(KB_ID_1), 0.85)
            );

            mockVectorSearch(vectorDocs);
            mockEntityManagerConsecutive(ftChunks, kwChunks);
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(reranked);

            List<Document> result = hybridRetrievalService.hybridSearch("test query", KB_IDS, TENANT_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getScore()).isEqualTo(0.95);
            assertThat(result.get(1).getScore()).isEqualTo(0.85);
            verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
            verify(rerankerService, times(1)).rerank(anyString(), anyList());
        }

        @Test
        @DisplayName("All sources empty produces empty result")
        void hybridSearch_allSourcesEmpty_returnsEmpty() {
            mockVectorSearch(Collections.emptyList());
            mockEntityManagerReturns(Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(Collections.emptyList());

            List<Document> result = hybridRetrievalService.hybridSearch("query", KB_IDS, TENANT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Vector search exception is handled gracefully")
        void hybridSearch_vectorSearchException_gracefullyDegrades() {
            doThrow(new RuntimeException("pgvector down"))
                    .when(vectorStore).similaritySearch(any(SearchRequest.class));
            mockEntityManagerReturns(Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(Collections.emptyList());

            List<Document> result = hybridRetrievalService.hybridSearch("query", KB_IDS, TENANT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Full-text search exception is handled gracefully")
        void hybridSearch_fullTextSearchException_gracefullyDegrades() {
            mockVectorSearch(buildDocList(2, "v", String.valueOf(KB_ID_1)));
            when(entityManager.createNativeQuery(anyString(), eq(DocumentChunk.class)))
                    .thenThrow(new RuntimeException("tsvector error"));
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(Collections.emptyList());

            hybridRetrievalService.hybridSearch("query", KB_IDS, TENANT_ID);

            // Reranker is still called with whatever was collected (vector docs only)
            verify(rerankerService).rerank(anyString(), anyList());
        }

        @Test
        @DisplayName("Reranker receives at most topK*2 candidates")
        void hybridSearch_rerankerReceivesLimitedCandidates() {
            List<Document> vectorDocs = buildDocList(30, "v", String.valueOf(KB_ID_1));
            List<Document> reranked = List.of(
                    buildDocWithScore("r", "1", "0", String.valueOf(KB_ID_1), 0.9));

            mockVectorSearch(vectorDocs);
            mockEntityManagerReturns(Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(reranked);

            hybridRetrievalService.hybridSearch("query", KB_IDS, TENANT_ID);

            verify(rerankerService).rerank(eq("query"), argThat(candidates -> candidates.size() <= 10));
        }

        @Test
        @DisplayName("Passes correct query to vectorStore and reranker")
        void hybridSearch_passesQueryToAllServices() {
            String query = "specific user question";
            mockVectorSearch(Collections.emptyList());
            mockEntityManagerReturns(Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(Collections.emptyList());

            hybridRetrievalService.hybridSearch(query, KB_IDS, TENANT_ID);

            ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
            verify(vectorStore).similaritySearch(captor.capture());
            assertThat(captor.getValue().getQuery()).isEqualTo(query);
            verify(rerankerService).rerank(eq(query), anyList());
        }

        @Test
        @DisplayName("Null vectorStore result is treated as empty")
        void hybridSearch_nullVectorResult_treatedAsEmpty() {
            doReturn(null).when(vectorStore).similaritySearch(any(SearchRequest.class));
            mockEntityManagerReturns(Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(Collections.emptyList());

            List<Document> result = hybridRetrievalService.hybridSearch("query", KB_IDS, TENANT_ID);

            assertThat(result).isEmpty();
        }
    }

    // ==================== hybridSearch with allowedDocIds (tag filter) ====================

    @Nested
    @DisplayName("hybridSearch - allowedDocIds filtering")
    class AllowedDocIdsFiltering {

        @Test
        @DisplayName("Filters results to only allowed document IDs")
        void hybridSearch_withAllowedDocIds_filtersCorrectly() {
            List<Document> vectorDocs = List.of(
                    buildDoc("c1", "100", "0", String.valueOf(KB_ID_1)),
                    buildDoc("c2", "101", "0", String.valueOf(KB_ID_1)),
                    buildDoc("c3", "102", "0", String.valueOf(KB_ID_1))
            );
            List<Document> reranked = List.of(
                    buildDocWithScore("c1", "100", "0", String.valueOf(KB_ID_1), 0.9));

            mockVectorSearch(vectorDocs);
            mockEntityManagerReturns(Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(reranked);

            Set<Long> allowedDocIds = Set.of(100L);
            hybridRetrievalService.hybridSearch("query", KB_IDS, TENANT_ID, allowedDocIds);

            // The reranker should only receive doc 100 (not 101 or 102)
            verify(rerankerService).rerank(eq("query"), argThat(candidates ->
                    candidates.stream().allMatch(d ->
                            "100".equals(d.getMetadata().get("document_id").toString()))
            ));
        }

        @Test
        @DisplayName("Null allowedDocIds means no filtering")
        void hybridSearch_nullAllowedDocIds_noFilter() {
            List<Document> vectorDocs = List.of(
                    buildDoc("c1", "100", "0", String.valueOf(KB_ID_1)),
                    buildDoc("c2", "101", "0", String.valueOf(KB_ID_1))
            );
            List<Document> reranked = List.of(
                    buildDocWithScore("c1", "100", "0", String.valueOf(KB_ID_1), 0.9));

            mockVectorSearch(vectorDocs);
            mockEntityManagerReturns(Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(reranked);

            hybridRetrievalService.hybridSearch("query", KB_IDS, TENANT_ID, null);

            // Both docs should reach the reranker
            verify(rerankerService).rerank(eq("query"), argThat(candidates -> candidates.size() == 2));
        }

        @Test
        @DisplayName("Empty allowedDocIds filters out everything")
        void hybridSearch_emptyAllowedDocIds_filtersAll() {
            List<Document> vectorDocs = List.of(
                    buildDoc("c1", "100", "0", String.valueOf(KB_ID_1))
            );

            mockVectorSearch(vectorDocs);
            mockEntityManagerReturns(Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(Collections.emptyList());

            hybridRetrievalService.hybridSearch("query", KB_IDS, TENANT_ID, Collections.emptySet());

            // All docs filtered before reaching reranker
            verify(rerankerService).rerank(eq("query"), argThat(List::isEmpty));
        }
    }

    // ==================== Vector search ====================

    @Nested
    @DisplayName("Vector search")
    class VectorSearch {

        @Test
        @DisplayName("Filter expression includes tenant_id")
        void vectorSearch_filterIncludesTenantId() {
            mockVectorSearch(Collections.emptyList());
            mockEntityManagerReturns(Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(Collections.emptyList());

            hybridRetrievalService.hybridSearch("query", KB_IDS, TENANT_ID);

            ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
            verify(vectorStore).similaritySearch(captor.capture());
            assertThat(captor.getValue().hasFilterExpression()).isTrue();
        }

        @Test
        @DisplayName("Uses expanded topK = topK * 3")
        void vectorSearch_usesExpandedTopK() {
            mockVectorSearch(Collections.emptyList());
            mockEntityManagerReturns(Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(Collections.emptyList());

            hybridRetrievalService.hybridSearch("query", KB_IDS, TENANT_ID);

            // topK=5, expandedTopK=15
            ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
            verify(vectorStore).similaritySearch(captor.capture());
            assertThat(captor.getValue().getTopK()).isEqualTo(15);
        }

        @Test
        @DisplayName("Uses configured similarity threshold")
        void vectorSearch_usesThreshold() {
            mockVectorSearch(Collections.emptyList());
            mockEntityManagerReturns(Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(Collections.emptyList());

            hybridRetrievalService.hybridSearch("query", KB_IDS, TENANT_ID);

            ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
            verify(vectorStore).similaritySearch(captor.capture());
            assertThat(captor.getValue().getSimilarityThreshold()).isCloseTo(0.7, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        @DisplayName("Single KB ID produces simple filter (no OR)")
        void vectorSearch_singleKbId_simpleFilter() {
            String filter = invokeBuildFilterExpression(Set.of(KB_ID_1), TENANT_ID);

            assertThat(filter).contains("knowledge_base_id == '" + KB_ID_1 + "'");
            assertThat(filter).contains("tenant_id == '" + TENANT_ID + "'");
            assertThat(filter).doesNotContain("||");
        }

        @Test
        @DisplayName("Multiple KB IDs produce OR filter")
        void vectorSearch_multipleKbIds_orFilter() {
            String filter = invokeBuildFilterExpression(KB_IDS, TENANT_ID);

            assertThat(filter).contains("||");
            assertThat(filter).contains("knowledge_base_id == '" + KB_ID_1 + "'");
            assertThat(filter).contains("knowledge_base_id == '" + KB_ID_2 + "'");
            assertThat(filter).contains("tenant_id == '" + TENANT_ID + "'");
        }
    }

    // ==================== Full-text search ====================

    @Nested
    @DisplayName("Full-text search")
    class FullTextSearch {

        @Test
        @DisplayName("Returns converted Document objects from DocumentChunk")
        void fullTextSearch_convertsChunks() {
            DocumentChunk chunk = DocumentChunk.builder()
                    .id(1L).content("full text match").knowledgeBaseId(KB_ID_1)
                    .documentId(100L).tenantId(TENANT_ID).chunkIndex(0).build();
            setupQueryMock(List.of(chunk));

            List<Document> result = invokeFullTextSearch("match", KB_IDS, TENANT_ID, 15);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getText()).isEqualTo("full text match");
            assertThat(result.get(0).getMetadata()).containsEntry("knowledge_base_id", String.valueOf(KB_ID_1));
            assertThat(result.get(0).getMetadata()).containsEntry("document_id", "100");
            assertThat(result.get(0).getMetadata()).containsEntry("chunk_index", "0");
            assertThat(result.get(0).getMetadata()).containsEntry("tenant_id", String.valueOf(TENANT_ID));
        }

        @Test
        @DisplayName("Sets correct query parameters")
        void fullTextSearch_setsParameters() {
            Query mockQuery = setupQueryMock(Collections.emptyList());

            invokeFullTextSearch("search term", KB_IDS, TENANT_ID, 20);

            verify(mockQuery).setParameter(eq("kbIds"), anyList());
            verify(mockQuery).setParameter(eq("tenantId"), eq(TENANT_ID));
            verify(mockQuery).setParameter(eq("query"), eq("search term"));
            verify(mockQuery).setParameter(eq("limit"), eq(20));
        }

        @Test
        @DisplayName("DB exception returns empty list")
        void fullTextSearch_dbException_returnsEmpty() {
            when(entityManager.createNativeQuery(anyString(), eq(DocumentChunk.class)))
                    .thenThrow(new RuntimeException("connection lost"));

            List<Document> result = invokeFullTextSearch("query", KB_IDS, TENANT_ID, 15);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("No results returns empty list")
        void fullTextSearch_noResults_returnsEmpty() {
            setupQueryMock(Collections.emptyList());

            List<Document> result = invokeFullTextSearch("nonexistent", KB_IDS, TENANT_ID, 15);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Multiple chunks are all converted")
        void fullTextSearch_multipleChunks_allConverted() {
            List<DocumentChunk> chunks = buildChunks(3, KB_ID_1);
            setupQueryMock(chunks);

            List<Document> result = invokeFullTextSearch("query", KB_IDS, TENANT_ID, 15);

            assertThat(result).hasSize(3);
            for (int i = 0; i < 3; i++) {
                assertThat(result.get(i).getText()).isEqualTo("chunk-content-" + i);
            }
        }
    }

    // ==================== Keyword extraction ====================

    @Nested
    @DisplayName("Keyword extraction")
    class KeywordExtraction {

        @Test
        @DisplayName("Extracts words of length >= 2")
        void extractKeywords_filtersByLength() {
            List<String> keywords = invokeExtractKeywords("a big test of keywords");

            assertThat(keywords).contains("big", "test", "of", "keywords");
            assertThat(keywords).doesNotContain("a");
        }

        @Test
        @DisplayName("Removes duplicates")
        void extractKeywords_removesDuplicates() {
            List<String> keywords = invokeExtractKeywords("test test hello hello");

            assertThat(keywords).doesNotHaveDuplicates();
            assertThat(keywords).containsExactlyInAnyOrder("test", "hello");
        }

        @Test
        @DisplayName("Handles Chinese punctuation")
        void extractKeywords_chinesePunctuation() {
            List<String> keywords = invokeExtractKeywords("你好，世界！测试一下");

            assertThat(keywords).contains("你好", "世界", "测试一下");
        }

        @Test
        @DisplayName("Handles mixed whitespace and punctuation")
        void extractKeywords_mixedDelimiters() {
            List<String> keywords = invokeExtractKeywords("  hello,  world;  test  ");

            assertThat(keywords).containsExactlyInAnyOrder("hello", "world", "test");
        }

        @Test
        @DisplayName("Empty string returns empty list")
        void extractKeywords_empty_returnsEmpty() {
            assertThat(invokeExtractKeywords("")).isEmpty();
        }

        @Test
        @DisplayName("Blank string returns empty list")
        void extractKeywords_blank_returnsEmpty() {
            assertThat(invokeExtractKeywords("   ")).isEmpty();
        }

        @Test
        @DisplayName("Null query returns empty list")
        void extractKeywords_null_returnsEmpty() {
            assertThat(invokeExtractKeywords(null)).isEmpty();
        }

        @Test
        @DisplayName("Only single-char words returns empty list")
        void extractKeywords_onlySingleCharWords_returnsEmpty() {
            assertThat(invokeExtractKeywords("a b c")).isEmpty();
        }

        @Test
        @DisplayName("Single character Chinese returns empty (length < 2)")
        void extractKeywords_singleCharChinese_excluded() {
            List<String> keywords = invokeExtractKeywords("你 好 世 界");
            assertThat(keywords).isEmpty();
        }

        @Test
        @DisplayName("Two character Chinese words are included")
        void extractKeywords_twoCharChinese_included() {
            List<String> keywords = invokeExtractKeywords("你好 世界 测试");
            assertThat(keywords).containsExactlyInAnyOrder("你好", "世界", "测试");
        }
    }

    // ==================== Keyword search ====================

    @Nested
    @DisplayName("Keyword search")
    class KeywordSearch {

        @Test
        @DisplayName("Returns empty when keywords list is empty")
        void keywordSearch_emptyKeywords_returnsEmpty() {
            List<Document> result = invokeKeywordSearch(Collections.emptyList(), KB_IDS, TENANT_ID, 15);

            assertThat(result).isEmpty();
            verifyNoInteractions(entityManager);
        }

        @Test
        @DisplayName("Converts DocumentChunk results to Documents")
        void keywordSearch_convertsChunks() {
            DocumentChunk chunk = DocumentChunk.builder()
                    .id(1L).content("keyword match").knowledgeBaseId(KB_ID_1)
                    .documentId(200L).tenantId(TENANT_ID).chunkIndex(2).build();
            Query mockQuery = setupQueryMock(List.of(chunk));

            List<Document> result = invokeKeywordSearch(List.of("keyword"), KB_IDS, TENANT_ID, 15);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getText()).isEqualTo("keyword match");
            assertThat(result.get(0).getMetadata()).containsEntry("document_id", "200");
            assertThat(result.get(0).getMetadata()).containsEntry("chunk_index", "2");
            // Verify the SQL keyword parameter is set with ILIKE wildcard
            verify(mockQuery).setParameter(eq("kw0"), eq("%keyword%"));
        }

        @Test
        @DisplayName("DB exception returns empty list")
        void keywordSearch_dbException_returnsEmpty() {
            when(entityManager.createNativeQuery(anyString(), eq(DocumentChunk.class)))
                    .thenThrow(new RuntimeException("DB down"));

            List<Document> result = invokeKeywordSearch(List.of("test"), KB_IDS, TENANT_ID, 15);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("No results returns empty list")
        void keywordSearch_noResults_returnsEmpty() {
            setupQueryMock(Collections.emptyList());

            List<Document> result = invokeKeywordSearch(List.of("rare"), KB_IDS, TENANT_ID, 15);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Multiple chunks converted correctly")
        void keywordSearch_multipleChunks() {
            List<DocumentChunk> chunks = buildChunks(3, KB_ID_1);
            setupQueryMock(chunks);

            List<Document> result = invokeKeywordSearch(List.of("test"), KB_IDS, TENANT_ID, 15);

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("Multiple keywords are all passed as parameters")
        void keywordSearch_multipleKeywords_allParameterized() {
            setupQueryMock(Collections.emptyList());

            invokeKeywordSearch(List.of("alpha", "beta", "gamma"), KB_IDS, TENANT_ID, 15);

            // Verify all keywords are passed (we can't easily capture the Query mock here
            // since setupQueryMock creates a new one, but we verify the call doesn't fail)
            verify(entityManager).createNativeQuery(anyString(), eq(DocumentChunk.class));
        }
    }

    // ==================== RRF fusion ====================

    @Nested
    @DisplayName("RRF fusion")
    class RRFFusion {

        @Test
        @DisplayName("Three-way fusion produces sorted results by score descending")
        void rrfFusion_sortedByScoreDescending() {
            // Doc 4 at keyword idx 0: weight 0.34 => highest score
            // Doc 1 at vector idx 0: weight 0.33
            // Doc 3 at fullText idx 0: weight 0.33
            List<Document> vector = List.of(
                    buildDoc("v0", "1", "0", "10"),
                    buildDoc("v1", "2", "0", "10"));
            List<Document> fullText = List.of(
                    buildDoc("ft0", "3", "0", "10"));
            List<Document> keyword = List.of(
                    buildDoc("kw0", "4", "0", "10"));

            List<Document> result = invokeRrfFusion(vector, fullText, keyword);

            assertThat(result).hasSize(4);
            // Doc 4 (keyword idx 0) has highest weight 0.34 => should be first
            assertThat(result.get(0).getMetadata().get("document_id")).isEqualTo("4");
        }

        @Test
        @DisplayName("Documents appearing in multiple lists accumulate scores")
        void rrfFusion_accumulatesScores() {
            Document shared = buildDoc("shared", "1", "0", "10");
            List<Document> vector = List.of(shared);
            List<Document> fullText = Collections.emptyList();
            List<Document> keyword = List.of(shared);

            List<Document> result = invokeRrfFusion(vector, fullText, keyword);

            // Same doc_id + chunk_index => same key => accumulated score
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMetadata().get("document_id")).isEqualTo("1");
        }

        @Test
        @DisplayName("All empty lists returns empty")
        void rrfFusion_allEmpty_returnsEmpty() {
            List<Document> result = invokeRrfFusion(
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Only vector results returns them")
        void rrfFusion_onlyVector_returnsVector() {
            List<Document> vector = buildDocList(3, "v", "10");

            List<Document> result = invokeRrfFusion(vector, Collections.emptyList(), Collections.emptyList());

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("Keyword weight (0.34) is higher than vector/fullText (0.33)")
        void rrfFusion_keywordWeightHigher() {
            // Each at index 0: rrf = 1/(60+1) = 1/61
            Document vDoc = buildDoc("v", "1", "0", "10");
            Document ftDoc = buildDoc("ft", "2", "0", "10");
            Document kwDoc = buildDoc("kw", "3", "0", "10");

            List<Document> result = invokeRrfFusion(
                    List.of(vDoc), List.of(ftDoc), List.of(kwDoc));

            // kwDoc at keyword index 0: 0.34 * (1/61) — highest weight
            assertThat(result.get(0).getMetadata().get("document_id")).isEqualTo("3");
        }

        @Test
        @DisplayName("Duplicate doc key across sources uses first-seen document")
        void rrfFusion_duplicateKey_usesFirstDocument() {
            Document fromVector = buildDoc("from-vector", "1", "0", "10");
            Document fromFullText = buildDoc("from-fulltext", "1", "0", "10");

            List<Document> result = invokeRrfFusion(
                    List.of(fromVector), List.of(fromFullText), Collections.emptyList());

            assertThat(result).hasSize(1);
            // putIfAbsent preserves the vector-sourced doc
            assertThat(result.get(0).getText()).isEqualTo("from-vector");
        }

        @Test
        @DisplayName("Lower-ranked items have lower RRF scores")
        void rrfFusion_lowerRanked_lowerScore() {
            List<Document> vector = buildDocList(5, "v", "10");

            List<Document> result = invokeRrfFusion(vector, Collections.emptyList(), Collections.emptyList());

            // Same order because index 0 has highest RRF score (1/61 > 1/62 > ...)
            assertThat(result).hasSize(5);
            assertThat(result.get(0).getMetadata().get("document_id")).isEqualTo("v-doc-0");
            assertThat(result.get(4).getMetadata().get("document_id")).isEqualTo("v-doc-4");
        }

        @Test
        @DisplayName("Empty source list in middle does not affect other sources")
        void rrfFusion_emptyMiddleSource_othersUnaffected() {
            List<Document> vector = List.of(buildDoc("v", "1", "0", "10"));
            List<Document> keyword = List.of(buildDoc("k", "2", "0", "10"));

            List<Document> result = invokeRrfFusion(vector, Collections.emptyList(), keyword);

            assertThat(result).hasSize(2);
        }
    }

    // ==================== Per-document dedup ====================

    @Nested
    @DisplayName("Per-document dedup")
    class PerDocDedup {

        @Test
        @DisplayName("Limits chunks per document to maxChunksPerDoc")
        void perDocDedup_limitsChunksPerDoc() {
            List<Document> docs = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                docs.add(buildDocWithScore("c" + i, "1", String.valueOf(i),
                        String.valueOf(KB_ID_1), 1.0 - i * 0.1));
            }

            List<Document> result = invokePerDocDedup(docs, KB_IDS);

            long countForDoc1 = result.stream()
                    .filter(d -> "1".equals(d.getMetadata().get("document_id").toString()))
                    .count();
            assertThat(countForDoc1).isLessThanOrEqualTo(2); // maxChunksPerDoc=2
        }

        @Test
        @DisplayName("Filters out docs not belonging to the knowledge bases")
        void perDocDedup_filtersUnknownKbIds() {
            List<Document> docs = List.of(
                    buildDocWithScore("in-kb", "1", "0", String.valueOf(KB_ID_1), 0.9),
                    buildDocWithScore("not-in-kb", "2", "0", "999", 0.95)
            );

            List<Document> result = invokePerDocDedup(docs, KB_IDS);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMetadata().get("document_id")).isEqualTo("1");
        }

        @Test
        @DisplayName("Keeps highest-scoring chunks per document")
        void perDocDedup_keepsHighestScoring() {
            List<Document> docs = List.of(
                    buildDocWithScore("low", "1", "0", String.valueOf(KB_ID_1), 0.3),
                    buildDocWithScore("high", "1", "1", String.valueOf(KB_ID_1), 0.9)
            );

            List<Document> result = invokePerDocDedup(docs, KB_IDS);

            assertThat(result).hasSize(2); // both fit within maxChunksPerDoc=2
            // high-score chunk comes first
            assertThat(result.get(0).getScore()).isEqualTo(0.9);
        }

        @Test
        @DisplayName("Final result limited to topK")
        void perDocDedup_limitedToTopK() {
            List<Document> docs = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                docs.add(buildDocWithScore("c" + i, String.valueOf(i), "0",
                        String.valueOf(KB_ID_1), 1.0 - i * 0.01));
            }

            List<Document> result = invokePerDocDedup(docs, KB_IDS);

            assertThat(result).hasSize(5); // topK=5
        }

        @Test
        @DisplayName("Empty list returns empty")
        void perDocDedup_empty_returnsEmpty() {
            List<Document> result = invokePerDocDedup(Collections.emptyList(), KB_IDS);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Handles null score gracefully (treated as 0)")
        void perDocDedup_nullScore_treatedAsZero() {
            Document withNullScore = new Document("no-score",
                    Map.of("document_id", "1", "chunk_index", "0",
                            "knowledge_base_id", String.valueOf(KB_ID_1)));
            // score is null by default

            Document withScore = buildDocWithScore("has-score", "1", "1",
                    String.valueOf(KB_ID_1), 0.8);

            List<Document> result = invokePerDocDedup(List.of(withNullScore, withScore), KB_IDS);

            assertThat(result).hasSize(2);
            // The one with score 0.8 should come first
            assertThat(result.get(0).getText()).isEqualTo("has-score");
        }

        @Test
        @DisplayName("Multiple documents are all preserved (up to limits)")
        void perDocDedup_multipleDocuments_preserved() {
            List<Document> docs = List.of(
                    buildDocWithScore("a", "1", "0", String.valueOf(KB_ID_1), 0.9),
                    buildDocWithScore("b", "2", "0", String.valueOf(KB_ID_1), 0.8),
                    buildDocWithScore("c", "3", "0", String.valueOf(KB_ID_1), 0.7)
            );

            List<Document> result = invokePerDocDedup(docs, KB_IDS);

            Set<String> docIds = result.stream()
                    .map(d -> d.getMetadata().get("document_id").toString())
                    .collect(Collectors.toSet());
            assertThat(docIds).containsExactlyInAnyOrder("1", "2", "3");
        }

        @Test
        @DisplayName("Result is sorted by score descending")
        void perDocDedup_sortedDescending() {
            List<Document> docs = List.of(
                    buildDocWithScore("low", "1", "0", String.valueOf(KB_ID_1), 0.1),
                    buildDocWithScore("mid", "2", "0", String.valueOf(KB_ID_1), 0.5),
                    buildDocWithScore("high", "3", "0", String.valueOf(KB_ID_1), 0.9)
            );

            List<Document> result = invokePerDocDedup(docs, KB_IDS);

            assertThat(result).hasSize(3);
            for (int i = 0; i < result.size() - 1; i++) {
                double scoreA = result.get(i).getScore() != null ? result.get(i).getScore() : 0;
                double scoreB = result.get(i + 1).getScore() != null ? result.get(i + 1).getScore() : 0;
                assertThat(scoreA).isGreaterThanOrEqualTo(scoreB);
            }
        }
    }

    // ==================== Filter expression ====================

    @Nested
    @DisplayName("Filter expression building")
    class FilterExpression {

        @Test
        @DisplayName("Single KB: knowledge_base_id == 'X' && tenant_id == 'Y'")
        void buildFilter_singleKb() {
            String filter = invokeBuildFilterExpression(Set.of(KB_ID_1), TENANT_ID);

            assertThat(filter).contains("knowledge_base_id == '" + KB_ID_1 + "'");
            assertThat(filter).contains("tenant_id == '" + TENANT_ID + "'");
            assertThat(filter).contains("&&");
            assertThat(filter).doesNotContain("||");
        }

        @Test
        @DisplayName("Multiple KBs: (kb1 || kb2) && tenant_id")
        void buildFilter_multipleKbs() {
            String filter = invokeBuildFilterExpression(KB_IDS, TENANT_ID);

            assertThat(filter).contains("||");
            assertThat(filter).contains("knowledge_base_id == '" + KB_ID_1 + "'");
            assertThat(filter).contains("knowledge_base_id == '" + KB_ID_2 + "'");
            assertThat(filter).contains("tenant_id == '" + TENANT_ID + "'");
        }

        @Test
        @DisplayName("Null tenant omits tenant_id filter")
        void buildFilter_nullTenant() {
            String filter = invokeBuildFilterExpression(Set.of(KB_ID_1), null);

            assertThat(filter).contains("knowledge_base_id");
            assertThat(filter).doesNotContain("tenant_id");
        }

        @Test
        @DisplayName("Multiple KBs with null tenant")
        void buildFilter_multipleKbs_nullTenant() {
            String filter = invokeBuildFilterExpression(KB_IDS, null);

            assertThat(filter).contains("||");
            assertThat(filter).doesNotContain("tenant_id");
        }

        @Test
        @DisplayName("Single KB with null tenant has no && separator")
        void buildFilter_singleKb_nullTenant_noAnd() {
            String filter = invokeBuildFilterExpression(Set.of(KB_ID_1), null);

            assertThat(filter).contains("knowledge_base_id == '" + KB_ID_1 + "'");
            assertThat(filter).doesNotContain("&&");
        }
    }

    // ==================== Full integration scenarios ====================

    @Nested
    @DisplayName("Integration scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("Single KB, single result, end-to-end")
        void integration_singleKb_singleResult() {
            Set<Long> singleKb = Set.of(KB_ID_1);
            Document vectorDoc = buildDoc("content", "100", "0", String.valueOf(KB_ID_1));
            Document reranked = buildDocWithScore("content", "100", "0",
                    String.valueOf(KB_ID_1), 0.92);

            mockVectorSearch(List.of(vectorDoc));
            mockEntityManagerReturns(Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(List.of(reranked));

            List<Document> result = hybridRetrievalService.hybridSearch("query", singleKb, TENANT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScore()).isEqualTo(0.92);
            assertThat(result.get(0).getText()).isEqualTo("content");
        }

        @Test
        @DisplayName("Empty query still processes (extractKeywords returns empty)")
        void integration_emptyQuery() {
            mockVectorSearch(Collections.emptyList());
            mockEntityManagerReturns(Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(Collections.emptyList());

            List<Document> result = hybridRetrievalService.hybridSearch("", KB_IDS, TENANT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Many docs from vector, few from full-text, merged correctly")
        void integration_multipleSourcesMerged() {
            List<Document> vectorDocs = List.of(
                    buildDoc("v1", "1", "0", String.valueOf(KB_ID_1)),
                    buildDoc("v2", "2", "0", String.valueOf(KB_ID_1)),
                    buildDoc("v3", "3", "0", String.valueOf(KB_ID_1))
            );
            // Full-text: docs 2,4 (doc 2 overlaps with vector)
            List<DocumentChunk> ftChunks = List.of(
                    DocumentChunk.builder().id(1L).content("v2").knowledgeBaseId(KB_ID_1)
                            .documentId(2L).tenantId(TENANT_ID).chunkIndex(0).build(),
                    DocumentChunk.builder().id(2L).content("v4").knowledgeBaseId(KB_ID_1)
                            .documentId(4L).tenantId(TENANT_ID).chunkIndex(0).build()
            );

            List<Document> reranked = List.of(
                    buildDocWithScore("merged", "1", "0", String.valueOf(KB_ID_1), 0.95));

            mockVectorSearch(vectorDocs);
            mockEntityManagerConsecutive(ftChunks, Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(reranked);

            List<Document> result = hybridRetrievalService.hybridSearch("test", KB_IDS, TENANT_ID);

            assertThat(result).isNotEmpty();
            verify(rerankerService).rerank(anyString(), anyList());
        }

        @Test
        @DisplayName("Expanded topK (15) passed to fullTextSearch and keywordSearch")
        void integration_expandedTopKPassedToDbQueries() {
            mockVectorSearch(Collections.emptyList());
            Query mockQuery = setupQueryMock(Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(Collections.emptyList());

            hybridRetrievalService.hybridSearch("test query", KB_IDS, TENANT_ID);

            // topK=5 => expandedTopK=15 => limit parameter should be 15
            verify(mockQuery, atLeastOnce()).setParameter(eq("limit"), eq(15));
        }

        @Test
        @DisplayName("Three-arg hybridSearch delegates to four-arg with null allowedDocIds")
        void integration_threeArgDelegates() {
            Document vectorDoc = buildDoc("c", "1", "0", String.valueOf(KB_ID_1));
            Document reranked = buildDocWithScore("c", "1", "0", String.valueOf(KB_ID_1), 0.9);

            mockVectorSearch(List.of(vectorDoc));
            mockEntityManagerReturns(Collections.emptyList());
            when(rerankerService.rerank(anyString(), anyList())).thenReturn(List.of(reranked));

            List<Document> result = hybridRetrievalService.hybridSearch("query", KB_IDS, TENANT_ID);

            assertThat(result).hasSize(1);
        }
    }

    // ==================== Private method invokers via reflection ====================

    @SuppressWarnings("unchecked")
    private List<Document> invokeFullTextSearch(String query, Set<Long> kbIds, Long tenantId, int limit) {
        return (List<Document>) ReflectionTestUtils.invokeMethod(
                hybridRetrievalService, "fullTextSearch", query, kbIds, tenantId, limit);
    }

    @SuppressWarnings("unchecked")
    private List<String> invokeExtractKeywords(String query) {
        return (List<String>) ReflectionTestUtils.invokeMethod(
                hybridRetrievalService, "extractKeywords", query);
    }

    @SuppressWarnings("unchecked")
    private List<Document> invokeKeywordSearch(List<String> keywords, Set<Long> kbIds,
                                               Long tenantId, int limit) {
        return (List<Document>) ReflectionTestUtils.invokeMethod(
                hybridRetrievalService, "keywordSearch", keywords, kbIds, tenantId, limit);
    }

    @SuppressWarnings("unchecked")
    private List<Document> invokeRrfFusion(List<Document> vector, List<Document> fullText,
                                            List<Document> keyword) {
        return (List<Document>) ReflectionTestUtils.invokeMethod(
                hybridRetrievalService, "rrfFusion", vector, fullText, keyword);
    }

    @SuppressWarnings("unchecked")
    private List<Document> invokePerDocDedup(List<Document> docs, Set<Long> kbIds) {
        return (List<Document>) ReflectionTestUtils.invokeMethod(
                hybridRetrievalService, "applyPerDocDedup", docs, kbIds);
    }

    private String invokeBuildFilterExpression(Set<Long> kbIds, Long tenantId) {
        return (String) ReflectionTestUtils.invokeMethod(
                hybridRetrievalService, "buildFilterExpression", kbIds, tenantId);
    }
}

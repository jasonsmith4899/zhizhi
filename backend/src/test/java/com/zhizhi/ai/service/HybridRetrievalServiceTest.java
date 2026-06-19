package com.zhizhi.ai.service;

import com.zhizhi.ai.model.entity.DocumentChunk;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HybridRetrievalService Unit Tests")
class HybridRetrievalServiceTest {

    @InjectMocks
    private HybridRetrievalService hybridRetrievalService;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private RerankerService rerankerService;

    @Mock
    private EntityManager entityManager;

    private final Long TENANT_ID = 1L;
    private final Long KB_ID_1 = 10L;
    private final Long KB_ID_2 = 20L;
    private final Set<Long> KB_IDS = Set.of(KB_ID_1, KB_ID_2);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(hybridRetrievalService, "alpha", 0.5);
        ReflectionTestUtils.setField(hybridRetrievalService, "rrfK", 60);
        ReflectionTestUtils.setField(hybridRetrievalService, "topK", 5);
        ReflectionTestUtils.setField(hybridRetrievalService, "threshold", 0.7);
        ReflectionTestUtils.setField(hybridRetrievalService, "maxChunksPerDoc", 2);
    }

    @Nested
    @DisplayName("Hybrid Search - hybridSearch")
    class HybridSearch {

        @Test
        @DisplayName("Hybrid search completes successfully")
        void testHybridSearchSuccess() {
            String query = "test query";
            List<Document> vectorResults = createDocuments(3, "vector");
            List<Document> rerankResults = createDocuments(2, "rerank");

            when(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .thenReturn(vectorResults);
            Query mockQuery = mock(Query.class);
            when(mockQuery.getResultList()).thenReturn(Collections.emptyList());
            when(entityManager.createNativeQuery(anyString(), eq(DocumentChunk.class)))
                    .thenReturn(mockQuery);

            when(rerankerService.rerank(anyString(), any(List.class)))
                    .thenReturn(rerankResults);

            List<Document> result = hybridRetrievalService.hybridSearch(query, KB_IDS, TENANT_ID);

            assertThat(result).isNotNull();
            verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
            verify(rerankerService, times(1)).rerank(anyString(), any(List.class));
        }

        @Test
        @DisplayName("Empty query returns empty results")
        void testHybridSearchWithEmptyQuery() {
            String query = "";
            when(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .thenReturn(Collections.emptyList());

            List<Document> result = hybridRetrievalService.hybridSearch(query, KB_IDS, TENANT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Null query returns empty results")
        void testHybridSearchWithNullQuery() {
            when(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .thenReturn(Collections.emptyList());

            List<Document> result = hybridRetrievalService.hybridSearch(null, KB_IDS, TENANT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Empty knowledge base IDs return empty results")
        void testHybridSearchWithEmptyKnowledgeBaseIds() {
            String query = "test query";
            Set<Long> emptyKbIds = Collections.emptySet();
            when(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .thenReturn(Collections.emptyList());

            List<Document> result = hybridRetrievalService.hybridSearch(query, emptyKbIds, TENANT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("VectorStore exception is handled gracefully")
        void testHybridSearchWithVectorStoreException() {
            String query = "test query";
            when(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .thenThrow(new RuntimeException("Vector store error"));

            List<Document> result = hybridRetrievalService.hybridSearch(query, KB_IDS, TENANT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Filter expression includes tenant_id for multi-tenant isolation")
        void testHybridSearchIncludesTenantIdFilter() {
            String query = "test query";
            List<Document> documents = createDocuments(2, "test");
            when(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .thenReturn(documents);
            when(rerankerService.rerank(anyString(), any(List.class)))
                    .thenReturn(Collections.emptyList());

            hybridRetrievalService.hybridSearch(query, KB_IDS, TENANT_ID);

            verify(vectorStore).similaritySearch(argThat(request ->
                    request.getFilterExpression() != null &&
                            request.getFilterExpression().contains("tenant_id")
            ));
        }
    }

    @Nested
    @DisplayName("Full Text Search - fullTextSearch")
    class FullTextSearch {

        @Test
        @DisplayName("Full text search returns converted DocumentChunk list")
        void testFullTextSearchSuccess() {
            String query = "test";
            List<DocumentChunk> chunks = createDocumentChunks(3);
            Query mockQuery = mock(Query.class);
            when(mockQuery.getResultList()).thenReturn(chunks);
            when(entityManager.createNativeQuery(anyString(), eq(DocumentChunk.class)))
                    .thenReturn(mockQuery);

            List<Document> result = callFullTextSearch(query, KB_IDS, TENANT_ID, 15);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getMetadata()).containsEntry("knowledge_base_id", "10");
        }

        @Test
        @DisplayName("Full text search query parameters are correct")
        void testFullTextSearchQueryParameters() {
            String query = "test query";
            Query mockQuery = mock(Query.class);
            when(mockQuery.getResultList()).thenReturn(Collections.emptyList());
            when(entityManager.createNativeQuery(anyString(), eq(DocumentChunk.class)))
                    .thenReturn(mockQuery);

            callFullTextSearch(query, KB_IDS, TENANT_ID, 15);

            verify(entityManager, times(1)).createNativeQuery(anyString(), eq(DocumentChunk.class));
            verify(mockQuery).setParameter("tenantId", TENANT_ID);
            verify(mockQuery).setParameter("query", query);
            verify(mockQuery).setParameter("limit", 15);
        }

        @Test
        @DisplayName("Database exception returns empty list")
        void testFullTextSearchWithException() {
            String query = "test query";
            when(entityManager.createNativeQuery(anyString(), eq(DocumentChunk.class)))
                    .thenThrow(new RuntimeException("Database error"));

            List<Document> result = callFullTextSearch(query, KB_IDS, TENANT_ID, 15);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("No results returns empty list")
        void testFullTextSearchWithNoResults() {
            String query = "nonexistent";
            Query mockQuery = mock(Query.class);
            when(mockQuery.getResultList()).thenReturn(Collections.emptyList());
            when(entityManager.createNativeQuery(anyString(), eq(DocumentChunk.class)))
                    .thenReturn(mockQuery);

            List<Document> result = callFullTextSearch(query, KB_IDS, TENANT_ID, 15);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Results preserve tenant_id in metadata")
        void testFullTextSearchPreservesTenantId() {
            String query = "test";
            List<DocumentChunk> chunks = createDocumentChunks(1);
            Query mockQuery = mock(Query.class);
            when(mockQuery.getResultList()).thenReturn(chunks);
            when(entityManager.createNativeQuery(anyString(), eq(DocumentChunk.class)))
                    .thenReturn(mockQuery);

            List<Document> result = callFullTextSearch(query, KB_IDS, TENANT_ID, 15);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMetadata()).containsEntry("tenant_id", String.valueOf(TENANT_ID));
        }
    }

    @Nested
    @DisplayName("Keyword Extraction - extractKeywords")
    class KeywordExtraction {

        @Test
        @DisplayName("Extract keywords from query")
        void testExtractKeywords() {
            String query = "test search query";

            List<String> keywords = callExtractKeywords(query);

            assertThat(keywords).isNotEmpty();
            assertThat(keywords).allMatch(kw -> kw.length() >= 2);
        }

        @Test
        @DisplayName("Handle Chinese punctuation")
        void testExtractKeywordsWithChinesePunctuation() {
            String query = "test query with punctuation. More content!";

            List<String> keywords = callExtractKeywords(query);

            assertThat(keywords).isNotEmpty();
        }

        @Test
        @DisplayName("Remove duplicate keywords")
        void testExtractKeywordsRemovesDuplicates() {
            String query = "test test query query";

            List<String> keywords = callExtractKeywords(query);

            assertThat(keywords).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("Empty query returns empty list")
        void testExtractKeywordsWithEmptyQuery() {
            String query = "";

            List<String> keywords = callExtractKeywords(query);

            assertThat(keywords).isEmpty();
        }

        @Test
        @DisplayName("Null query returns empty list")
        void testExtractKeywordsWithNullQuery() {
            String query = null;

            List<String> keywords = callExtractKeywords(query);

            assertThat(keywords).isEmpty();
        }

        @Test
        @DisplayName("Filter out short words")
        void testExtractKeywordsFiltersShortWords() {
            String query = "a test b query we";

            List<String> keywords = callExtractKeywords(query);

            assertThat(keywords).allMatch(kw -> kw.length() >= 2);
        }
    }

    @Nested
    @DisplayName("RRF Fusion - rrfFusion")
    class RRFFusion {

        @Test
        @DisplayName("Three-way fusion calculates scores correctly")
        void testRRFFusionCalculatesScores() {
            List<Document> vectorResults = createDocumentsWithIds(3, "vector");
            List<Document> fullTextResults = createDocumentsWithIds(2, "fulltext");
            List<Document> keywordResults = createDocumentsWithIds(2, "keyword");

            List<Document> result = callRRFFusion(vectorResults, fullTextResults, keywordResults);

            assertThat(result).isNotEmpty();
            for (int i = 0; i < result.size() - 1; i++) {
                assertThat(result.get(i).getScore())
                        .isGreaterThanOrEqualTo(result.get(i + 1).getScore());
            }
        }

        @Test
        @DisplayName("Single result list fusion")
        void testRRFFusionWithSingleList() {
            List<Document> vectorResults = createDocumentsWithIds(3, "vector");
            List<Document> emptyList = Collections.emptyList();

            List<Document> result = callRRFFusion(vectorResults, emptyList, emptyList);

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("All empty lists returns empty")
        void testRRFFusionWithEmptyLists() {
            List<Document> emptyList = Collections.emptyList();

            List<Document> result = callRRFFusion(emptyList, emptyList, emptyList);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Duplicate documents accumulate fusion scores")
        void testRRFFusionAccumulatesScoresForDuplicates() {
            Document doc1 = createDocumentWithId("doc-1");
            Document doc2 = createDocumentWithId("doc-2");
            List<Document> vectorResults = List.of(doc1, doc2);
            List<Document> fullTextResults = List.of(doc1);

            List<Document> result = callRRFFusion(vectorResults, fullTextResults, Collections.emptyList());

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getMetadata().get("document_id")).isEqualTo("doc-1");
        }

        @Test
        @DisplayName("Large lists are fused with deduplication")
        void testRRFFusionWithLargeLists() {
            List<Document> vectorResults = createDocumentsWithIds(50, "vector");
            List<Document> fullTextResults = createDocumentsWithIds(50, "fulltext");
            List<Document> keywordResults = createDocumentsWithIds(50, "keyword");

            List<Document> result = callRRFFusion(vectorResults, fullTextResults, keywordResults);

            assertThat(result).isNotEmpty();
            Set<String> resultIds = result.stream()
                    .map(d -> d.getMetadata().get("document_id").toString())
                    .collect(Collectors.toSet());
            assertThat(resultIds.size()).isLessThan(150);
        }
    }

    @Nested
    @DisplayName("Per-document Deduplication - applyPerDocDedup")
    class PerDocDedup {

        @Test
        @DisplayName("Group by document_id and limit chunks")
        void testPerDocDedupGroupsAndLimits() {
            List<Document> docs = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("knowledge_base_id", "1");
                meta.put("document_id", "doc-1");
                meta.put("chunk_index", String.valueOf(i));
                Document d = new Document("content" + i, meta);
                d.setScore(1.0 - (i * 0.1));
                docs.add(d);
            }

            List<Document> result = callApplyPerDocDedup(docs, KB_IDS);

            long doc1Count = result.stream()
                    .filter(d -> d.getMetadata().get("document_id").equals("doc-1"))
                    .count();
            assertThat(doc1Count).isLessThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Filter out documents not in knowledge base")
        void testPerDocDedupFiltersUnknownKnowledgeBases() {
            List<Document> docs = new ArrayList<>();
            Map<String, Object> meta1 = new HashMap<>();
            meta1.put("knowledge_base_id", "1");
            meta1.put("document_id", "doc-1");
            docs.add(new Document("content1", meta1));

            Map<String, Object> meta2 = new HashMap<>();
            meta2.put("knowledge_base_id", "999");
            meta2.put("document_id", "doc-2");
            docs.add(new Document("content2", meta2));

            List<Document> result = callApplyPerDocDedup(docs, KB_IDS);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMetadata().get("knowledge_base_id")).isEqualTo("1");
        }

        @Test
        @DisplayName("Keep highest scoring chunks per document")
        void testPerDocDedupKeepsHighestScoring() {
            List<Document> docs = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("knowledge_base_id", "1");
                meta.put("document_id", "doc-1");
                meta.put("chunk_index", String.valueOf(i));
                Document d = new Document("content" + i, meta);
                d.setScore(0.5 + (i * 0.2));
                docs.add(d);
            }

            List<Document> result = callApplyPerDocDedup(docs, KB_IDS);

            assertThat(result).hasSizeGreaterThanOrEqualTo(1);
            assertThat(result.get(0).getScore()).isGreaterThanOrEqualTo(0.7);
        }

        @Test
        @DisplayName("Final results limited to topK")
        void testPerDocDedupLimitsFinalResults() {
            List<Document> docs = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("knowledge_base_id", "1");
                meta.put("document_id", "doc-" + (i % 10));
                meta.put("chunk_index", String.valueOf(i / 10));
                Document d = new Document("content" + i, meta);
                d.setScore(1.0 - (i * 0.02));
                docs.add(d);
            }

            List<Document> result = callApplyPerDocDedup(docs, KB_IDS);

            assertThat(result).hasSize(5);
        }

        @Test
        @DisplayName("Empty document list returns empty")
        void testPerDocDedupWithEmptyList() {
            List<Document> docs = Collections.emptyList();

            List<Document> result = callApplyPerDocDedup(docs, KB_IDS);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Filter Expression Building - buildFilterExpression")
    class FilterExpressionBuilding {

        @Test
        @DisplayName("Single knowledge base ID generates correct filter")
        void testBuildFilterExpressionWithSingleKB() {
            Set<Long> kbIds = Set.of(10L);
            Long tenantId = 1L;

            String filterExpr = callBuildFilterExpression(kbIds, tenantId);

            assertThat(filterExpr).contains("knowledge_base_id");
            assertThat(filterExpr).contains("tenant_id");
            assertThat(filterExpr).contains("10");
        }

        @Test
        @DisplayName("Multiple knowledge base IDs generate OR filter")
        void testBuildFilterExpressionWithMultipleKBs() {
            Set<Long> kbIds = Set.of(10L, 20L, 30L);
            Long tenantId = 1L;

            String filterExpr = callBuildFilterExpression(kbIds, tenantId);

            assertThat(filterExpr).contains("||");
            assertThat(filterExpr).contains("knowledge_base_id");
            assertThat(filterExpr).contains("tenant_id");
        }

        @Test
        @DisplayName("Filter includes tenant_id to prevent data leakage")
        void testBuildFilterExpressionIncludesTenantId() {
            Set<Long> kbIds = Set.of(10L);
            Long tenantId = 123L;

            String filterExpr = callBuildFilterExpression(kbIds, tenantId);

            assertThat(filterExpr).contains("tenant_id == '123'");
        }

        @Test
        @DisplayName("Null tenant_id still builds filter")
        void testBuildFilterExpressionWithNullTenantId() {
            Set<Long> kbIds = Set.of(10L);

            String filterExpr = callBuildFilterExpression(kbIds, null);

            assertThat(filterExpr).contains("knowledge_base_id");
        }

        @Test
        @DisplayName("SQL injection protection via quote escaping")
        void testBuildFilterExpressionEscapesSqlInjection() {
            Set<Long> kbIds = Set.of(10L);
            Long tenantId = 1L;

            String filterExpr = callBuildFilterExpression(kbIds, tenantId);

            assertThat(filterExpr).doesNotContain("''");
        }
    }

    @Nested
    @DisplayName("Document Conversion")
    class DocumentConversion {

        @Test
        @DisplayName("DocumentChunk converts to Document correctly")
        void testDocumentChunkConversion() {
            String query = "test";
            DocumentChunk chunk = DocumentChunk.builder()
                    .id(1L)
                    .content("test content")
                    .knowledgeBaseId(10L)
                    .documentId(100L)
                    .tenantId(1L)
                    .chunkIndex(0)
                    .build();
            Query mockQuery = mock(Query.class);
            when(mockQuery.getResultList()).thenReturn(List.of(chunk));
            when(entityManager.createNativeQuery(anyString(), eq(DocumentChunk.class)))
                    .thenReturn(mockQuery);

            List<Document> result = callFullTextSearch(query, KB_IDS, TENANT_ID, 15);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getText()).isEqualTo("test content");
            assertThat(result.get(0).getMetadata()).containsEntry("knowledge_base_id", "10");
        }
    }

    // Helper methods for invoking private methods via reflection

    private List<Document> callFullTextSearch(String query, Set<Long> kbIds, Long tenantId, int limit) {
        try {
            return (List<Document>) ReflectionTestUtils
                    .invokeMethod(hybridRetrievalService, "fullTextSearch",
                            query, kbIds, tenantId, limit);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> callExtractKeywords(String query) {
        try {
            return (List<String>) ReflectionTestUtils
                    .invokeMethod(hybridRetrievalService, "extractKeywords", query);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Document> callRRFFusion(List<Document> vectorResults,
                                         List<Document> fullTextResults,
                                         List<Document> keywordResults) {
        try {
            return (List<Document>) ReflectionTestUtils
                    .invokeMethod(hybridRetrievalService, "rrfFusion",
                            vectorResults, fullTextResults, keywordResults);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Document> callApplyPerDocDedup(List<Document> docs, Set<Long> kbIds) {
        try {
            return (List<Document>) ReflectionTestUtils
                    .invokeMethod(hybridRetrievalService, "applyPerDocDedup", docs, kbIds);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String callBuildFilterExpression(Set<Long> kbIds, Long tenantId) {
        try {
            return (String) ReflectionTestUtils
                    .invokeMethod(hybridRetrievalService, "buildFilterExpression", kbIds, tenantId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Document creation helper methods

    private List<Document> createDocuments(int count, String prefix) {
        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("knowledge_base_id", String.valueOf(KB_ID_1));
            metadata.put("document_id", prefix + "-doc-" + i);
            metadata.put("chunk_index", String.valueOf(i));
            metadata.put("tenant_id", String.valueOf(TENANT_ID));
            Document doc = new Document("content " + prefix + " " + i, metadata);
            doc.setScore(1.0 - (i * 0.1));
            docs.add(doc);
        }
        return docs;
    }

    private List<Document> createDocumentsWithIds(int count, String prefix) {
        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("knowledge_base_id", String.valueOf(KB_ID_1));
            metadata.put("document_id", prefix + "-doc-" + i);
            metadata.put("chunk_index", String.valueOf(i));
            metadata.put("tenant_id", String.valueOf(TENANT_ID));
            Document doc = new Document("content " + prefix + " " + i, metadata);
            docs.add(doc);
        }
        return docs;
    }

    private Document createDocumentWithId(String docId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("knowledge_base_id", String.valueOf(KB_ID_1));
        metadata.put("document_id", docId);
        metadata.put("chunk_index", "0");
        metadata.put("tenant_id", String.valueOf(TENANT_ID));
        return new Document("content " + docId, metadata);
    }

    private List<DocumentChunk> createDocumentChunks(int count) {
        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chunks.add(DocumentChunk.builder()
                    .id((long) i)
                    .documentId(100L + i)
                    .knowledgeBaseId(KB_ID_1)
                    .tenantId(TENANT_ID)
                    .chunkIndex(i)
                    .content("content " + i)
                    .build());
        }
        return chunks;
    }
}

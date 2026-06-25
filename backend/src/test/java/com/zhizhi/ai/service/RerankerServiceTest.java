package com.zhizhi.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RerankerServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RerankerService rerankerService;

    @BeforeEach
    void setUp() {
        rerankerService = new RerankerService(restTemplate, objectMapper);
        setField(rerankerService, "apiKey", "test-api-key");
        setField(rerankerService, "baseUrl", "https://api.siliconflow.cn");
        setField(rerankerService, "model", "BAAI/bge-reranker-v2-m3");
        setField(rerankerService, "topK", 5);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Document createDoc(String text, String docId, String chunkIdx, double score) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("document_id", docId);
        metadata.put("chunk_index", chunkIdx);
        metadata.put("knowledge_base_id", "1");
        return Document.builder().text(text).metadata(metadata).score(score).build();
    }

    // ==================== Happy path ====================

    @Test
    void rerank_happyPath_returnsRerankedInScoreOrder() {
        List<Document> candidates = List.of(
                createDoc("first doc", "1", "0", 0.6),
                createDoc("second doc", "2", "0", 0.5),
                createDoc("third doc", "3", "0", 0.7)
        );

        String apiResponse = "{\"results\":[" +
                "{\"index\":2,\"score\":0.95}," +
                "{\"index\":0,\"score\":0.85}," +
                "{\"index\":1,\"score\":0.75}" +
                "]}";
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(apiResponse);

        List<Document> result = rerankerService.rerank("test query", candidates);

        assertThat(result).hasSize(3);
        // Sorted by reranker score descending
        assertThat(result.get(0).getText()).isEqualTo("third doc");
        assertThat(result.get(0).getScore()).isEqualTo(0.95);
        assertThat(result.get(1).getText()).isEqualTo("first doc");
        assertThat(result.get(1).getScore()).isEqualTo(0.85);
        assertThat(result.get(2).getText()).isEqualTo("second doc");
        assertThat(result.get(2).getScore()).isEqualTo(0.75);
    }

    @Test
    void rerank_preservesOriginalMetadata() {
        List<Document> candidates = List.of(
                createDoc("doc content", "10", "3", 0.5)
        );

        String apiResponse = "{\"results\":[{\"index\":0,\"score\":0.92}]}";
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(apiResponse);

        List<Document> result = rerankerService.rerank("query", candidates);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMetadata()).containsEntry("document_id", "10");
        assertThat(result.get(0).getMetadata()).containsEntry("chunk_index", "3");
        assertThat(result.get(0).getMetadata()).containsEntry("knowledge_base_id", "1");
    }

    @Test
    void rerank_limitsResultsToTopK() {
        List<Document> candidates = new ArrayList<>();
        StringBuilder resultsJson = new StringBuilder("{\"results\":[");
        for (int i = 0; i < 10; i++) {
            candidates.add(createDoc("doc " + i, String.valueOf(i), "0", 0.5));
            if (i > 0) resultsJson.append(",");
            resultsJson.append("{\"index\":").append(i).append(",\"score\":").append(1.0 - i * 0.05).append("}");
        }
        resultsJson.append("]}");

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(resultsJson.toString());

        List<Document> result = rerankerService.rerank("query", candidates);

        assertThat(result).hasSize(5); // limited by topK=5
    }

    // ==================== Empty / blank apiKey ====================

    @Test
    void rerank_emptyCandidates_returnsEmpty() {
        List<Document> result = rerankerService.rerank("query", Collections.emptyList());
        assertThat(result).isEmpty();
        verifyNoInteractions(restTemplate);
    }

    @Test
    void rerank_blankApiKey_returnsOriginalOrderLimitedToTopK() {
        setField(rerankerService, "apiKey", "");

        List<Document> candidates = List.of(
                createDoc("doc1", "1", "0", 0.6),
                createDoc("doc2", "2", "0", 0.7),
                createDoc("doc3", "3", "0", 0.5)
        );

        List<Document> result = rerankerService.rerank("query", candidates);

        assertThat(result).hasSize(3);
        // Returns original order, limited to topK
        assertThat(result.get(0).getText()).isEqualTo("doc1");
        assertThat(result.get(1).getText()).isEqualTo("doc2");
        assertThat(result.get(2).getText()).isEqualTo("doc3");
        verifyNoInteractions(restTemplate);
    }

    @Test
    void rerank_blankApiKey_limitsToTopK() {
        setField(rerankerService, "apiKey", "  ");  // whitespace only is also blank

        List<Document> candidates = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            candidates.add(createDoc("doc" + i, String.valueOf(i), "0", 0.5));
        }

        List<Document> result = rerankerService.rerank("query", candidates);

        assertThat(result).hasSize(5); // limited by topK=5
    }

    // ==================== API failure ====================

    @Test
    void rerank_apiFailure_returnsOriginalOrderLimitedToTopK() {
        List<Document> candidates = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            candidates.add(createDoc("doc" + i, String.valueOf(i), "0", 0.5));
        }

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        List<Document> result = rerankerService.rerank("query", candidates);

        // Falls back to original order, limited to topK=5
        assertThat(result).hasSize(5);
        assertThat(result.get(0).getText()).isEqualTo("doc0");
    }

    @Test
    void rerank_apiReturnsInvalidJson_returnsOriginalOrder() {
        List<Document> candidates = List.of(
                createDoc("doc1", "1", "0", 0.6)
        );

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn("not valid json{{{");

        List<Document> result = rerankerService.rerank("query", candidates);

        // Falls back to original order
        assertThat(result).hasSize(1);
    }

    // ==================== Malformed response ====================

    @Test
    void rerank_missingResultsField_returnsOriginalOrder() {
        List<Document> candidates = List.of(
                createDoc("doc1", "1", "0", 0.6),
                createDoc("doc2", "2", "0", 0.5)
        );

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn("{\"data\":\"something\"}");

        List<Document> result = rerankerService.rerank("query", candidates);

        assertThat(result).hasSize(2);
    }

    @Test
    void rerank_resultMissingIndexOrScore_skipsEntry() {
        List<Document> candidates = List.of(
                createDoc("doc1", "1", "0", 0.6),
                createDoc("doc2", "2", "0", 0.5)
        );

        // First result has no "score", second is valid
        String apiResponse = "{\"results\":[" +
                "{\"index\":0}," +
                "{\"index\":1,\"score\":0.9}" +
                "]}";
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(apiResponse);

        List<Document> result = rerankerService.rerank("query", candidates);

        // Only the valid entry is included
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getText()).isEqualTo("doc2");
        assertThat(result.get(0).getScore()).isEqualTo(0.9);
    }

    @Test
    void rerank_resultIndexOutOfBounds_skipsEntry() {
        List<Document> candidates = List.of(
                createDoc("doc1", "1", "0", 0.6)
        );

        // Index 5 is out of bounds (only 1 candidate)
        String apiResponse = "{\"results\":[{\"index\":5,\"score\":0.9}]}";
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(apiResponse);

        List<Document> result = rerankerService.rerank("query", candidates);

        assertThat(result).isEmpty();
    }

    @Test
    void rerank_negativeIndex_skipsEntry() {
        List<Document> candidates = List.of(
                createDoc("doc1", "1", "0", 0.6)
        );

        String apiResponse = "{\"results\":[{\"index\":-1,\"score\":0.9}]}";
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(apiResponse);

        List<Document> result = rerankerService.rerank("query", candidates);

        assertThat(result).isEmpty();
    }

    // ==================== API call verification ====================

    @Test
    void rerank_callsCorrectUrl() {
        List<Document> candidates = List.of(createDoc("doc1", "1", "0", 0.5));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn("{\"results\":[{\"index\":0,\"score\":0.9}]}");

        rerankerService.rerank("query", candidates);

        verify(restTemplate).postForObject(
                eq("https://api.siliconflow.cn/v1/rerank"),
                any(HttpEntity.class),
                eq(String.class)
        );
    }
}

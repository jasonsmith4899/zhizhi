package com.zhizhi.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RerankerService Unit Tests")
class RerankerServiceTest {

    private RerankerService rerankerService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private final String API_KEY = "test-api-key-123";
    private final String BASE_URL = "https://api.siliconflow.cn";
    private final String MODEL = "BAAI/bge-reranker-v2-m3";
    private final int TOP_K = 5;

    @BeforeEach
    void setUp() {
        rerankerService = new RerankerService(restTemplate, objectMapper);
        ReflectionTestUtils.setField(rerankerService, "apiKey", API_KEY);
        ReflectionTestUtils.setField(rerankerService, "baseUrl", BASE_URL);
        ReflectionTestUtils.setField(rerankerService, "model", MODEL);
        ReflectionTestUtils.setField(rerankerService, "topK", TOP_K);
    }

    @Nested
    @DisplayName("Rerank Basic Functionality")
    class RerankBasic {

        @Test
        @DisplayName("Successfully rerank and return documents sorted by score")
        void testRerankSuccess() throws Exception {
            String query = "test query";
            List<Document> candidates = createCandidates(3);

            JsonNode mockResponse = createMockRerankResponse(3);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"mock\": \"request\"}");
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn("{\"results\": [{\"index\": 0, \"score\": 0.95}, {\"index\": 1, \"score\": 0.87}, {\"index\": 2, \"score\": 0.72}]}");
            when(objectMapper.readTree(anyString())).thenReturn(mockResponse);

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).isNotEmpty();
            assertThat(result.size()).isLessThanOrEqualTo(TOP_K);
            for (int i = 0; i < result.size() - 1; i++) {
                assertThat(result.get(i).getScore()).isGreaterThanOrEqualTo(result.get(i + 1).getScore());
            }
            verify(restTemplate, times(1)).postForObject(anyString(), any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("Empty candidates return empty list")
        void testRerankWithEmptyCandidates() {
            String query = "test query";
            List<Document> candidates = Collections.emptyList();

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).isEmpty();
            verify(restTemplate, never()).postForObject(anyString(), any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("Missing API Key returns original order")
        void testRerankWithoutApiKey() {
            String query = "test query";
            List<Document> candidates = createCandidates(10);
            ReflectionTestUtils.setField(rerankerService, "apiKey", "");

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).hasSize(TOP_K);
            assertThat(result).isEqualTo(candidates.stream().limit(TOP_K).toList());
            verify(restTemplate, never()).postForObject(anyString(), any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("Blank API Key returns original order")
        void testRerankWithBlankApiKey() {
            String query = "test query";
            List<Document> candidates = createCandidates(10);
            ReflectionTestUtils.setField(rerankerService, "apiKey", "   ");

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).hasSize(TOP_K);
            verify(restTemplate, never()).postForObject(anyString(), any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("Fewer candidates than topK returns all")
        void testRerankWithFewerCandidatesThanTopK() throws Exception {
            String query = "test query";
            List<Document> candidates = createCandidates(2);

            JsonNode mockResponse = createMockRerankResponse(2);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"mock\": \"request\"}");
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn("{\"results\": [{\"index\": 0, \"score\": 0.95}, {\"index\": 1, \"score\": 0.87}]}");
            when(objectMapper.readTree(anyString())).thenReturn(mockResponse);

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("API Call and Parsing")
    class ApiCallAndParsing {

        @Test
        @DisplayName("API exception falls back to original order")
        void testRerankWithApiException() throws Exception {
            String query = "test query";
            List<Document> candidates = createCandidates(3);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"mock\": \"request\"}");
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new RestClientException("Connection timeout"));

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).hasSize(3);
            assertThat(result).isEqualTo(candidates);
        }

        @Test
        @DisplayName("JSON parse exception falls back to original order")
        void testRerankWithJsonParseException() throws Exception {
            String query = "test query";
            List<Document> candidates = createCandidates(3);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"mock\": \"request\"}");
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn("invalid json");
            when(objectMapper.readTree(anyString())).thenThrow(new RuntimeException("Invalid JSON"));

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).hasSize(TOP_K);
        }

        @Test
        @DisplayName("Missing results field returns original order")
        void testRerankWithMissingResultsField() throws Exception {
            String query = "test query";
            List<Document> candidates = createCandidates(3);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"mock\": \"request\"}");
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn("{\"error\": \"something went wrong\"}");

            ObjectMapper realObjectMapper = new ObjectMapper();
            ReflectionTestUtils.setField(rerankerService, "objectMapper", realObjectMapper);

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).hasSize(TOP_K);
        }

        @Test
        @DisplayName("Out of bounds index in results is ignored")
        void testRerankWithOutOfBoundsIndex() throws Exception {
            String query = "test query";
            List<Document> candidates = createCandidates(2);

            ObjectMapper realObjectMapper = new ObjectMapper();
            ReflectionTestUtils.setField(rerankerService, "objectMapper", realObjectMapper);

            String responseJson = "{\"results\": [{\"index\": 0, \"score\": 0.95}, {\"index\": 10, \"score\": 0.87}]}";
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(responseJson);

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScore()).isEqualTo(0.95);
        }

        @Test
        @DisplayName("Null score in results is handled correctly")
        void testRerankWithNullScore() throws Exception {
            String query = "test query";
            List<Document> candidates = createCandidates(2);

            ObjectMapper realObjectMapper = new ObjectMapper();
            ReflectionTestUtils.setField(rerankerService, "objectMapper", realObjectMapper);

            String responseJson = "{\"results\": [{\"index\": 0, \"score\": 0.95}, {\"index\": 1, \"score\": null}]}";
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(responseJson);

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Request Building")
    class RequestBuilding {

        @Test
        @DisplayName("Request contains all required fields")
        void testBuildRequestContainsRequiredFields() throws Exception {
            String query = "test query";
            List<Document> candidates = createCandidates(3);

            ObjectMapper realObjectMapper = new ObjectMapper();
            ReflectionTestUtils.setField(rerankerService, "objectMapper", realObjectMapper);

            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn("{\"results\": []}");

            rerankerService.rerank(query, candidates);

            verify(restTemplate, times(1)).postForObject(
                    argThat(url -> url.contains("/v1/rerank")),
                    argThat(entity -> {
                        String body = entity.getBody();
                        return body != null && body.contains("model") && body.contains("query")
                                && body.contains("documents") && body.contains("top_n");
                    }),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("Request URL contains base URL and endpoint")
        void testBuildRequestUrlCorrect() throws Exception {
            String query = "test";
            List<Document> candidates = createCandidates(1);

            ObjectMapper realObjectMapper = new ObjectMapper();
            ReflectionTestUtils.setField(rerankerService, "objectMapper", realObjectMapper);

            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn("{\"results\": []}");

            rerankerService.rerank(query, candidates);

            verify(restTemplate).postForObject(
                    eq(BASE_URL + "/v1/rerank"),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("Request headers have correct Content-Type and auth")
        void testBuildRequestHeadersCorrect() throws Exception {
            String query = "test";
            List<Document> candidates = createCandidates(1);

            ObjectMapper realObjectMapper = new ObjectMapper();
            ReflectionTestUtils.setField(rerankerService, "objectMapper", realObjectMapper);

            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn("{\"results\": []}");

            rerankerService.rerank(query, candidates);

            verify(restTemplate).postForObject(
                    anyString(),
                    argThat(entity -> {
                        String contentType = entity.getHeaders().getContentType().toString();
                        String authHeader = entity.getHeaders().getFirst("Authorization");
                        return contentType.contains("application/json")
                                && authHeader != null && authHeader.contains("Bearer");
                    }),
                    eq(String.class)
            );
        }
    }

    @Nested
    @DisplayName("Document Processing and Metadata")
    class DocumentProcessing {

        @Test
        @DisplayName("Reranked documents preserve original metadata")
        void testRerankPreservesMetadata() throws Exception {
            String query = "test query";
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("knowledge_base_id", "1");
            metadata.put("document_id", "doc-123");
            metadata.put("tenant_id", "tenant-1");
            Document doc = new Document("test content", metadata);
            List<Document> candidates = List.of(doc);

            ObjectMapper realObjectMapper = new ObjectMapper();
            ReflectionTestUtils.setField(rerankerService, "objectMapper", realObjectMapper);

            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn("{\"results\": [{\"index\": 0, \"score\": 0.95}]}");

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMetadata()).containsAllEntriesOf(metadata);
        }

        @Test
        @DisplayName("Reranked documents have updated score")
        void testRerankUpdatesScore() throws Exception {
            String query = "test query";
            Document doc = new Document("test content", new HashMap<>());
            List<Document> candidates = List.of(doc);

            ObjectMapper realObjectMapper = new ObjectMapper();
            ReflectionTestUtils.setField(rerankerService, "objectMapper", realObjectMapper);

            double expectedScore = 0.95;
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn("{\"results\": [{\"index\": 0, \"score\": " + expectedScore + "}]}");

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScore()).isEqualTo(expectedScore);
        }

        @Test
        @DisplayName("Multiple documents correctly sorted by score")
        void testRerankSortsMultipleDocumentsByScore() throws Exception {
            String query = "test query";
            List<Document> candidates = createCandidates(5);

            ObjectMapper realObjectMapper = new ObjectMapper();
            ReflectionTestUtils.setField(rerankerService, "objectMapper", realObjectMapper);

            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn("{\"results\": [" +
                            "{\"index\": 2, \"score\": 0.99}," +
                            "{\"index\": 0, \"score\": 0.95}," +
                            "{\"index\": 4, \"score\": 0.82}," +
                            "{\"index\": 1, \"score\": 0.77}," +
                            "{\"index\": 3, \"score\": 0.65}" +
                            "]}");

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).hasSize(5);
            assertThat(result.get(0).getScore()).isEqualTo(0.99);
            assertThat(result.get(1).getScore()).isEqualTo(0.95);
            assertThat(result.get(2).getScore()).isEqualTo(0.82);
            assertThat(result.get(3).getScore()).isEqualTo(0.77);
            assertThat(result.get(4).getScore()).isEqualTo(0.65);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Candidates exceeding topK are truncated")
        void testRerankTruncatesExcessCandidates() throws Exception {
            String query = "test query";
            List<Document> candidates = createCandidates(20);

            ObjectMapper realObjectMapper = new ObjectMapper();
            ReflectionTestUtils.setField(rerankerService, "objectMapper", realObjectMapper);

            StringBuilder responseJson = new StringBuilder("{\"results\": [");
            for (int i = 0; i < 20; i++) {
                if (i > 0) responseJson.append(",");
                responseJson.append("{\"index\": ").append(i).append(", \"score\": ").append(1.0 - (i * 0.05)).append("}");
            }
            responseJson.append("]}");

            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(responseJson.toString());

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).hasSize(TOP_K);
        }

        @Test
        @DisplayName("Duplicate indices are handled correctly")
        void testRerankWithDuplicateIndices() throws Exception {
            String query = "test query";
            List<Document> candidates = createCandidates(3);

            ObjectMapper realObjectMapper = new ObjectMapper();
            ReflectionTestUtils.setField(rerankerService, "objectMapper", realObjectMapper);

            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn("{\"results\": [{\"index\": 0, \"score\": 0.95}, {\"index\": 0, \"score\": 0.90}]}");

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result.size()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Empty results array returns empty list")
        void testRerankWithEmptyResults() throws Exception {
            String query = "test query";
            List<Document> candidates = createCandidates(3);

            ObjectMapper realObjectMapper = new ObjectMapper();
            ReflectionTestUtils.setField(rerankerService, "objectMapper", realObjectMapper);

            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn("{\"results\": []}");

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Multi-tenant Isolation")
    class TenantIsolation {

        @Test
        @DisplayName("Different tenant documents preserve metadata")
        void testRerankPreservesTenantId() throws Exception {
            String query = "test query";
            Map<String, Object> metadata1 = new HashMap<>();
            metadata1.put("tenant_id", "tenant-1");
            metadata1.put("document_id", "doc-1");

            Map<String, Object> metadata2 = new HashMap<>();
            metadata2.put("tenant_id", "tenant-2");
            metadata2.put("document_id", "doc-2");

            List<Document> candidates = List.of(
                    new Document("content1", metadata1),
                    new Document("content2", metadata2)
            );

            ObjectMapper realObjectMapper = new ObjectMapper();
            ReflectionTestUtils.setField(rerankerService, "objectMapper", realObjectMapper);

            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn("{\"results\": [{\"index\": 0, \"score\": 0.95}, {\"index\": 1, \"score\": 0.87}]}");

            List<Document> result = rerankerService.rerank(query, candidates);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getMetadata().get("tenant_id")).isEqualTo("tenant-1");
            assertThat(result.get(1).getMetadata().get("tenant_id")).isEqualTo("tenant-2");
        }
    }

    // Helper methods

    private List<Document> createCandidates(int count) {
        List<Document> candidates = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("knowledge_base_id", "1");
            metadata.put("document_id", "doc-" + i);
            metadata.put("chunk_index", String.valueOf(i));
            metadata.put("tenant_id", "tenant-1");
            candidates.add(new Document("candidate content " + i, metadata));
        }
        return candidates;
    }

    private JsonNode createMockRerankResponse(int resultCount) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode response = mapper.createObjectNode();
        ArrayNode results = mapper.createArrayNode();

        for (int i = 0; i < resultCount; i++) {
            ObjectNode result = mapper.createObjectNode();
            result.put("index", i);
            result.put("score", 1.0 - (i * 0.08));
            results.add(result);
        }

        response.set("results", results);
        return response;
    }
}

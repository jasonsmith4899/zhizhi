package com.zhizhi.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RerankerService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${SILICONFLOW_API_KEY:}")
    private String apiKey;

    @Value("${RERANKER_BASE_URL:https://api.siliconflow.cn}")
    private String baseUrl;

    @Value("${RERANKER_MODEL:BAAI/bge-reranker-v2-m3}")
    private String model;

    @Value("${app.ai.similarity-top-k:5}")
    private int topK;

    /**
     * 使用 bge-reranker-v2-m3 对候选文档重排
     *
     * @param query 用户查询
     * @param candidates 候选文档（通常是混合检索返回的topK*3个）
     * @return 重排后的文档列表（按相关性降序，最多topK个）
     */
    public List<Document> rerank(String query, List<Document> candidates) {
        if (candidates.isEmpty() || apiKey.isBlank()) {
            log.warn("Reranking 跳过：candidates={}, apiKeyConfigured={}",
                    candidates.size(), !apiKey.isBlank());
            return candidates.stream().limit(topK).collect(Collectors.toList());
        }

        try {
            // 1. 构建请求
            Map<String, Object> request = buildRequest(query, candidates);

            // 2. 调用 SiliconFlow API
            JsonNode response = callRerankApi(request);

            // 3. 解析响应并更新文档分数
            return parseRerankResponse(response, candidates);

        } catch (Exception e) {
            log.warn("Reranking 失败，返回原排序: {}", e.getMessage());
            return candidates.stream().limit(topK).collect(Collectors.toList());
        }
    }

    private Map<String, Object> buildRequest(String query, List<Document> candidates) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("query", query);

        // 构建文档列表
        List<Map<String, String>> docs = candidates.stream()
                .map(doc -> {
                    Map<String, String> docMap = new LinkedHashMap<>();
                    docMap.put("text", doc.getText());
                    return docMap;
                })
                .collect(Collectors.toList());
        request.put("documents", docs);
        request.put("top_n", topK);

        return request;
    }

    private JsonNode callRerankApi(Map<String, Object> request) throws Exception {
        String url = baseUrl + "/v1/rerank";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String requestBody = objectMapper.writeValueAsString(request);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        log.debug("Reranker request to {}: {}", url, requestBody);

        String responseBody = restTemplate.postForObject(url, entity, String.class);
        JsonNode response = objectMapper.readTree(responseBody);

        log.debug("Reranker response: {}", responseBody);

        return response;
    }

    private List<Document> parseRerankResponse(JsonNode response, List<Document> candidates) {
        List<Document> reranked = new ArrayList<>();

        // 预期响应格式：{ "results": [ { "index": 0, "score": 0.95, ... }, ... ] }
        JsonNode results = response.get("results");
        if (results == null || !results.isArray()) {
            log.warn("Reranker 响应格式异常，缺少 results 字段");
            return candidates.stream().limit(topK).collect(Collectors.toList());
        }

        results.forEach(result -> {
            JsonNode indexNode = result.get("index");
            JsonNode scoreNode = result.get("score");
            if (indexNode == null || scoreNode == null) {
                log.warn("Reranker 响应中缺少必要字段");
                return;
            }

            int index = indexNode.asInt();
            double score = scoreNode.asDouble();

            if (index >= 0 && index < candidates.size()) {
                Document doc = candidates.get(index);
                // 创建新的 Document 对象，保留原有 metadata，使用 Reranker 得分
                Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
                Document rerankDoc = Document.builder()
                        .text(doc.getText())
                        .metadata(metadata)
                        .score(score)
                        .build();
                reranked.add(rerankDoc);
            }
        });

        // 按分数降序排列
        reranked.sort((a, b) -> Double.compare(
                b.getScore() != null ? b.getScore() : 0,
                a.getScore() != null ? a.getScore() : 0
        ));

        return reranked;
    }
}

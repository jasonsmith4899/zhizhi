package com.zhizhi.ai.service;

import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.dto.ChatRequest;
import com.zhizhi.ai.model.dto.ChatResponse;
import com.zhizhi.ai.model.entity.ApiKey;
import com.zhizhi.ai.model.entity.Conversation;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import com.zhizhi.ai.model.entity.Message;
import com.zhizhi.ai.repository.ApiKeyRepository;
import com.zhizhi.ai.repository.ConversationRepository;
import com.zhizhi.ai.repository.DocumentTagRepository;
import com.zhizhi.ai.repository.KnowledgeBaseRepository;
import com.zhizhi.ai.repository.MessageRepository;
import com.zhizhi.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private record RagContext(String augmentedPrompt, String systemPrompt,
                              List<ChatResponse.SourceReference> sources,
                              List<Map<String, Object>> kagTriples) {}

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final HybridRetrievalService hybridRetrievalService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final DocumentTagRepository documentTagRepository;
    private final ConversationRepository conversationRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final AuthUtil authUtil;
    private final ChatMemory chatMemory;

    @Value("${app.ai.similarity-top-k:5}")
    private int topK;

    @Value("${app.ai.similarity-threshold:0.7}")
    private double threshold;

    @Value("${app.ai.max-chunks-per-doc:2}")
    private int maxChunksPerDoc;

    @Value("${app.ai.recall-min-score:0.5}")
    private double recallMinScore;

    @Value("${app.ai.recall-min-chunks:1}")
    private int recallMinChunks;

    @Value("${app.ai.max-context-messages:10}")
    private int maxContextMessages;

    @Value("${app.chat.sse-timeout:60000}")
    private long sseTimeout;

    @Value("${app.chat.max-conversations:50}")
    private int maxConversations;

    @Value("${app.chat.max-messages:100}")
    private int maxMessages;

    {
        // 初始化 ChatClient (在构造器之后)
    }

    private ChatClient getChatClient() {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是「智知」AI知识库助手。请遵循以下规则：
                        1. 根据知识库中的参考信息回答用户问题
                        2. 如果参考信息中没有相关内容，请诚实说明"知识库中暂无相关信息"
                        3. 回答要准确、简洁、专业
                        4. 在回答末尾可以标注信息来源
                        5. 不要编造不确定的信息
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * 从 API Key 解析系统提示词和知识库 IDs
     */
    private record ApiKeyConfig(String systemPrompt, Set<Long> knowledgeBaseIds) {}

    private ApiKeyConfig resolveApiKeyConfig(ChatRequest request) {
        Long apiKeyId = request.getApiKeyId();

        if (apiKeyId != null) {
            ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                    .orElseThrow(() -> BusinessException.notFound("API Key"));

            // 组装系统提示词
            String persona = apiKey.getAssistantPersona() != null ? apiKey.getAssistantPersona() : "";
            String background = apiKey.getMerchantBackground() != null ? apiKey.getMerchantBackground() : "";
            String rules = apiKey.getAnswerRules() != null ? apiKey.getAnswerRules() : "";
            String systemPrompt = Stream.of(persona, background, rules)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining("\n\n"));
            if (systemPrompt.isBlank()) systemPrompt = null;

            // 获取关联的知识库 IDs
            Set<Long> kbIds = apiKey.getKnowledgeBases() != null
                    ? apiKey.getKnowledgeBases().stream().map(KnowledgeBase::getId).collect(Collectors.toSet())
                    : Set.of();

            return new ApiKeyConfig(systemPrompt, kbIds);
        }

        // 兼容模式：直接使用 knowledgeBaseId
        Long kbId = request.getKnowledgeBaseId();
        if (kbId != null) {
            return new ApiKeyConfig(null, Set.of(kbId));
        }

        throw BusinessException.badRequest("请提供 apiKeyId 或 knowledgeBaseId");
    }

    @Transactional
    public ChatResponse chat(ChatRequest request, Long userId, Authentication authentication) {
        ApiKeyConfig config = resolveApiKeyConfig(request);
        if (config.knowledgeBaseIds().isEmpty()) {
            throw BusinessException.badRequest("该 API Key 未关联任何知识库，请先在设置中配置");
        }

        Conversation conversation = getOrCreateConversation(request, userId);
        String sessionId = conversation.getSessionId();
        Long tenantId = conversation.getTenantId();

        Message userMsg = Message.builder()
                .conversationId(conversation.getId())
                .tenantId(tenantId)
                .role("user")
                .content(request.getMessage())
                .build();
        messageRepository.save(userMsg);

        String reply;
        List<ChatResponse.SourceReference> sources = new ArrayList<>();

        try {
            RagContext ragCtx = buildRagContext(request.getMessage(), config.knowledgeBaseIds(), config.systemPrompt(), request.getTagIds());
            sources = ragCtx.sources();

            // 召回不满足要求，生成礼貌拒绝回复
            if (ragCtx.augmentedPrompt() == null) {
                log.info("Recall not satisfied for message: {}", request.getMessage());
                reply = generatePoliteRejection(request.getMessage(), config.systemPrompt());
            } else {
                ChatClient ragClient;
                if (ragCtx.systemPrompt() != null) {
                    ragClient = ChatClient.builder(chatModel)
                            .defaultSystem(ragCtx.systemPrompt())
                            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                            .build();
                } else {
                    ragClient = getChatClient();
                }
                reply = ragClient
                        .prompt()
                        .user(ragCtx.augmentedPrompt())
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                        .call()
                        .content();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("RAG chat failed", e);
            throw new BusinessException("对话失败，请稍后重试");
        }

        String sourcesJson = sources.isEmpty() ? null : toJson(sources);
        Message assistantMsg = Message.builder()
                .conversationId(conversation.getId())
                .tenantId(tenantId)
                .role("assistant")
                .content(reply)
                .sourceDocuments(sourcesJson)
                .build();
        messageRepository.save(assistantMsg);

        conversation.setMessageCount(conversation.getMessageCount() + 2);
        if (conversation.getTitle() == null) {
            conversation.setTitle(request.getMessage().substring(0,
                    Math.min(request.getMessage().length(), 50)));
        }
        conversationRepository.save(conversation);

        return ChatResponse.builder()
                .reply(reply)
                .sessionId(sessionId)
                .messageId(assistantMsg.getId().toString())
                .sources(sources)
                .build();
    }

    public SseEmitter streamChat(ChatRequest request, Long userId, Authentication authentication) {
        ApiKeyConfig config = resolveApiKeyConfig(request);
        if (config.knowledgeBaseIds().isEmpty()) {
            throw BusinessException.badRequest("该 API Key 未关联任何知识库，请先在设置中配置");
        }

        SseEmitter emitter = new SseEmitter(sseTimeout);

        Conversation conversation = getOrCreateConversation(request, userId);
        String sessionId = conversation.getSessionId();
        Long tenantId = conversation.getTenantId();

        Message userMsg = Message.builder()
                .conversationId(conversation.getId())
                .tenantId(tenantId)
                .role("user")
                .content(request.getMessage())
                .build();
        messageRepository.save(userMsg);

        List<ChatResponse.SourceReference> sources = new ArrayList<>();
        StringBuilder fullReply = new StringBuilder();

        try {
            RagContext ragCtx = buildRagContext(request.getMessage(), config.knowledgeBaseIds(), config.systemPrompt(), request.getTagIds());
            String augmentedPrompt = ragCtx.augmentedPrompt();
            String systemPrompt = ragCtx.systemPrompt();
            sources.addAll(ragCtx.sources());
            final List<ChatResponse.SourceReference> finalSources = sources;
            final List<Map<String, Object>> finalKagTriples = ragCtx.kagTriples();

            // Send sessionId first
            emitter.send(SseEmitter.event()
                    .name("session")
                    .data("{\"sessionId\":\"" + sessionId + "\"}"));

            // 召回不满足要求，生成礼貌拒绝回复
            if (augmentedPrompt == null) {
                log.info("Recall not satisfied for stream message: {}", request.getMessage());
                String rejectionReply = generatePoliteRejection(request.getMessage(), config.systemPrompt());
                fullReply.append(rejectionReply);
                emitter.send(SseEmitter.event()
                        .name("chunk")
                        .data(rejectionReply));
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(""));

                Message assistantMsg = Message.builder()
                        .conversationId(conversation.getId())
                        .tenantId(tenantId)
                        .role("assistant")
                        .content(rejectionReply)
                        .sourceDocuments(null)
                        .build();
                messageRepository.save(assistantMsg);

                conversation.setMessageCount(conversation.getMessageCount() + 2);
                if (conversation.getTitle() == null) {
                    conversation.setTitle(request.getMessage().substring(0,
                            Math.min(request.getMessage().length(), 50)));
                }
                conversationRepository.save(conversation);

                emitter.complete();
            } else {
                ChatClient.Builder clientBuilder;
                if (systemPrompt != null) {
                    clientBuilder = ChatClient.builder(chatModel)
                            .defaultSystem(systemPrompt)
                            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
                } else {
                    clientBuilder = getChatClient().mutate();
                }

                // Stream response
                String finalAugmentedPrompt = augmentedPrompt;
                ChatClient.Builder finalClientBuilder = clientBuilder;
                finalClientBuilder.build()
                        .prompt()
                        .user(finalAugmentedPrompt)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                        .stream()
                        .chatResponse()
                        .doOnNext(chunk -> {
                            try {
                                String content = chunk.getResult().getOutput().getText();
                                if (content != null && !content.isEmpty()) {
                                    fullReply.append(content);
                                    emitter.send(SseEmitter.event()
                                            .name("chunk")
                                            .data(content));
                                }
                            } catch (Exception e) {
                                log.error("Error sending chunk", e);
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                if (!finalSources.isEmpty()) {
                                    emitter.send(SseEmitter.event()
                                            .name("sources")
                                            .data(toJson(finalSources)));
                                }

                                // KAG 结构化三元组（在 sources 之后、done 之前发送）
                                // 已在 buildRagContext 阶段预计算，此处直接使用
                                if (finalKagTriples != null && !finalKagTriples.isEmpty()) {
                                    try {
                                        emitter.send(SseEmitter.event()
                                                .name("kag_sources")
                                                .data(toJsonMap(finalKagTriples)));
                                    } catch (Exception kagEx) {
                                        log.warn("Failed to send kag_sources event", kagEx);
                                    }
                                }

                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data(""));

                                Message assistantMsg = Message.builder()
                                        .conversationId(conversation.getId())
                                        .tenantId(tenantId)
                                        .role("assistant")
                                        .content(fullReply.toString())
                                        .sourceDocuments(finalSources.isEmpty() ? null : toJson(finalSources))
                                        .build();
                                messageRepository.save(assistantMsg);

                                conversation.setMessageCount(conversation.getMessageCount() + 2);
                                if (conversation.getTitle() == null) {
                                    conversation.setTitle(request.getMessage().substring(0,
                                            Math.min(request.getMessage().length(), 50)));
                                }
                                conversationRepository.save(conversation);

                                emitter.complete();
                            } catch (Exception e) {
                                log.error("Error completing stream", e);
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnError(e -> {
                            log.error("Stream error", e);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data("生成回复时发生错误"));
                            } catch (Exception ex) {
                                log.warn("Failed to send error event", ex);
                            }
                            emitter.completeWithError(e);
                        })
                        .subscribe();
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to start stream", e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("初始化对话失败"));
            } catch (Exception ex) {
                log.warn("Failed to send init error event", ex);
            }
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private RagContext buildRagContext(String message, Set<Long> knowledgeBaseIds, String systemPrompt, List<Long> tagIds) {
        Long tenantId = TenantContext.getTenantId();

        // 校验所有知识库的访问权限
        for (Long kbId : knowledgeBaseIds) {
            var kb = knowledgeBaseRepository.findById(kbId)
                    .orElseThrow(() -> BusinessException.notFound("知识库"));
            if (tenantId != null && !kb.getTenantId().equals(tenantId)) {
                throw BusinessException.forbidden("无权访问此知识库");
            }
        }

        // 标签过滤：解析出带有指定标签的文档范围（空集表示该标签下无文档）
        Set<Long> allowedDocIds = null;
        if (tagIds != null && !tagIds.isEmpty()) {
            allowedDocIds = new HashSet<>(documentTagRepository.findDocumentIdsByTagIds(tenantId, tagIds));
        }

        // 混合检索：向量搜索 + 关键词搜索 → RRF 融合 → per-doc 去重 → topK
        List<Document> filteredDocs = hybridRetrievalService.hybridSearch(
                message, knowledgeBaseIds, tenantId, allowedDocIds);

        // 检查召回质量：文档数量和最高分数
        boolean recallSatisfied = checkRecallQuality(filteredDocs);

        // KAG：检索 query 相关的知识图谱子图（双引擎之一，无命中返回 null）
        String subgraph = knowledgeGraphService.querySubgraph(message, knowledgeBaseIds, tenantId);
        boolean hasSubgraph = subgraph != null && !subgraph.isBlank();

        // KAG 结构化三元组（与子图同时获取，供 SSE kag_sources 事件使用）
        List<Map<String, Object>> kagTriples = knowledgeGraphService.querySubgraphStructured(message, knowledgeBaseIds, tenantId);

        String augmentedPrompt;
        if (!recallSatisfied && !hasSubgraph) {
            // RAG 与 KAG 均无有效召回，不使用知识库内容，让大模型礼貌拒绝
            augmentedPrompt = null;
        } else {
            StringBuilder sb = new StringBuilder("根据以下参考信息回答问题：\n\n");
            if (recallSatisfied) {
                String context = filteredDocs.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n---\n"));
                sb.append("【文档片段】\n").append(context).append("\n\n");
            }
            if (hasSubgraph) {
                sb.append("【知识图谱关系】\n").append(subgraph).append("\n\n");
            }
            sb.append("问题：").append(message);
            augmentedPrompt = sb.toString();
        }

        // 来源仅在向量召回满足时展示（KAG 子图作为推理增强，不单独列为来源）
        List<ChatResponse.SourceReference> sources = !recallSatisfied
                ? new ArrayList<ChatResponse.SourceReference>()
                : filteredDocs.stream()
                .map(doc -> {
                    Long docId = null;
                    Object docIdObj = doc.getMetadata().get("document_id");
                    if (docIdObj != null) {
                        try { docId = Long.parseLong(docIdObj.toString()); } catch (NumberFormatException ignored) {}
                    }
                    Integer chunkIdx = null;
                    Object chunkIdxObj = doc.getMetadata().get("chunk_index");
                    if (chunkIdxObj != null) {
                        try { chunkIdx = Integer.parseInt(chunkIdxObj.toString()); } catch (NumberFormatException ignored) {}
                    }
                    return ChatResponse.SourceReference.builder()
                            .documentId(docId)
                            .documentName((String) doc.getMetadata().get("filename"))
                            .content(doc.getText())
                            .score(doc.getScore())
                            .chunkIndex(chunkIdx)
                            .build();
                })
                .collect(Collectors.toList());

        return new RagContext(augmentedPrompt, systemPrompt, sources, kagTriples);
    }

    /**
     * 检查召回质量是否满足要求
     */
    private boolean checkRecallQuality(List<Document> docs) {
        if (docs.isEmpty()) {
            return false;
        }
        // 检查文档数量
        if (docs.size() < recallMinChunks) {
            return false;
        }
        // 检查最高分数
        double maxScore = docs.stream()
                .mapToDouble(doc -> doc.getScore() != null ? doc.getScore() : 0.0)
                .max()
                .orElse(0.0);
        return maxScore >= recallMinScore;
    }

    /**
     * 生成礼貌的拒绝回复（知识库中没有相关内容时）
     */
    private String generatePoliteRejection(String message, String systemPrompt) {
        String rejectionPrompt = """
                用户询问了一个问题，但知识库中没有找到相关内容。
                请礼貌地回复用户，说明你无法回答这个问题，因为知识库中暂无相关信息。
                建议用户换个问法或联系管理员添加相关知识。
                请用友好、专业的语气回复，不要编造答案。
                """;

        ChatClient rejectClient;
        if (systemPrompt != null) {
            rejectClient = ChatClient.builder(chatModel)
                    .defaultSystem(systemPrompt + "\n\n" + rejectionPrompt)
                    .build();
        } else {
            rejectClient = ChatClient.builder(chatModel)
                    .defaultSystem(rejectionPrompt)
                    .build();
        }

        return rejectClient
                .prompt()
                .user("用户问题：" + message)
                .call()
                .content();
    }

    private Conversation getOrCreateConversation(ChatRequest request, Long userId) {
        Long tenantId = TenantContext.getTenantId();
        if (request.getSessionId() != null) {
            if (tenantId != null) {
                return conversationRepository.findByTenantIdAndSessionId(tenantId, request.getSessionId())
                        .orElseThrow(() -> BusinessException.notFound("会话"));
            }
            return conversationRepository.findBySessionId(request.getSessionId())
                    .orElseThrow(() -> BusinessException.notFound("会话"));
        }

        if (tenantId == null) {
            throw BusinessException.badRequest("缺少租户信息");
        }

        // 从 request 获取 knowledgeBaseId（兼容模式）或从 apiKey 获取第一个
        Long kbId = request.getKnowledgeBaseId();
        if (kbId == null && request.getApiKeyId() != null) {
            ApiKey apiKey = apiKeyRepository.findById(request.getApiKeyId()).orElse(null);
            if (apiKey != null && apiKey.getKnowledgeBases() != null && !apiKey.getKnowledgeBases().isEmpty()) {
                kbId = apiKey.getKnowledgeBases().iterator().next().getId();
            }
        }

        Conversation conversation = Conversation.builder()
                .sessionId(UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .tenantId(tenantId)
                .knowledgeBaseId(kbId)
                .build();
        return conversationRepository.save(conversation);
    }

    public List<Conversation> listConversations(Long userId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return conversationRepository.findByTenantIdOrderByUpdatedAtDesc(
                    tenantId, org.springframework.data.domain.PageRequest.of(0, maxConversations)
            ).getContent();
        }
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(
                userId, org.springframework.data.domain.PageRequest.of(0, maxConversations)
        ).getContent();
    }

    public List<Message> getMessages(Long conversationId, Long userId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("会话"));
        if (!conv.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权访问此会话");
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return messageRepository.findByTenantIdAndConversationIdOrderByCreatedAtAsc(
                    tenantId, conversationId, org.springframework.data.domain.PageRequest.of(0, maxMessages)
            ).getContent();
        }
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(
                conversationId, org.springframework.data.domain.PageRequest.of(0, maxMessages)
        ).getContent();
    }

    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("会话"));
        if (!conv.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权删除此会话");
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !conv.getTenantId().equals(tenantId)) {
            throw BusinessException.forbidden("无权删除此会话");
        }
        conversationRepository.delete(conv);
    }

    private String toJson(List<ChatResponse.SourceReference> sources) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(sources);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String toJsonMap(List<Map<String, Object>> data) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
        } catch (Exception e) {
            return "[]";
        }
    }
}

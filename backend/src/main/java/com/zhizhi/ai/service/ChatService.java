package com.zhizhi.ai.service;

import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.TenantContext;
import com.zhizhi.ai.model.dto.ChatRequest;
import com.zhizhi.ai.model.dto.ChatResponse;
import com.zhizhi.ai.model.entity.ApiKey;
import com.zhizhi.ai.model.entity.Conversation;
import com.zhizhi.ai.model.entity.Message;
import com.zhizhi.ai.repository.ApiKeyRepository;
import com.zhizhi.ai.repository.ConversationRepository;
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
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatService {

    private record RagContext(String augmentedPrompt, String systemPrompt,
                              List<ChatResponse.SourceReference> sources) {}

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final VectorStore vectorStore;
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

    @Value("${app.ai.max-context-messages:10}")
    private int maxContextMessages;

    @Value("${app.chat.sse-timeout:60000}")
    private long sseTimeout;

    @Value("${app.chat.max-conversations:50}")
    private int maxConversations;

    @Value("${app.chat.max-messages:100}")
    private int maxMessages;

    public ChatService(
            ChatModel chatModel,
            VectorStore vectorStore,
            ConversationRepository conversationRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            ApiKeyRepository apiKeyRepository,
            AuthUtil authUtil,
            ChatMemory chatMemory) {
        this.vectorStore = vectorStore;
        this.conversationRepository = conversationRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.authUtil = authUtil;
        this.chatMemory = chatMemory;
        this.chatModel = chatModel;

        this.chatClient = ChatClient.builder(chatModel)
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
     * 校验 API Key 对知识库的访问权限
     */
    private void validateKnowledgeBaseAccess(Authentication authentication, Long knowledgeBaseId) {
        if (authentication == null || !authUtil.isApiKeyAuth(authentication)) {
            return; // JWT 认证不做额外校验
        }
        Long apiKeyId = authUtil.getApiKeyId(authentication);
        if (apiKeyId == null) return;

        ApiKey apiKey = apiKeyRepository.findById(apiKeyId).orElse(null);
        if (apiKey == null) return;

        if (!apiKey.canAccessKnowledgeBase(knowledgeBaseId)) {
            if (apiKey.getKnowledgeBases() == null || apiKey.getKnowledgeBases().isEmpty()) {
                throw BusinessException.forbidden("此 API Key 未关联任何知识库，请先在设置中配置");
            }
            throw BusinessException.forbidden("此 API Key 无权访问该知识库");
        }
    }

    @Transactional
    public ChatResponse chat(ChatRequest request, Long userId, Authentication authentication) {
        validateKnowledgeBaseAccess(authentication, request.getKnowledgeBaseId());
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

        // knowledgeBaseId 已通过 @NotNull 校验，必不为 null
        try {
            RagContext ragCtx = buildRagContext(request.getMessage(), request.getKnowledgeBaseId());
            sources = ragCtx.sources();

            ChatClient ragClient = chatClient;
            if (ragCtx.systemPrompt() != null) {
                ragClient = ChatClient.builder(chatModel)
                        .defaultSystem(ragCtx.systemPrompt())
                        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                        .build();
            }
            reply = ragClient
                    .prompt()
                    .user(ragCtx.augmentedPrompt())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .call()
                    .content();
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
        validateKnowledgeBaseAccess(authentication, request.getKnowledgeBaseId());
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
            String augmentedPrompt = request.getMessage();
            String systemPrompt = null;
            List<ChatResponse.SourceReference> ragSources = new ArrayList<>();

            // knowledgeBaseId 已通过 @NotNull 校验，必不为 null
            RagContext ragCtx = buildRagContext(request.getMessage(), request.getKnowledgeBaseId());
            augmentedPrompt = ragCtx.augmentedPrompt();
            systemPrompt = ragCtx.systemPrompt();
            ragSources.addAll(ragCtx.sources());
            final List<ChatResponse.SourceReference> finalSources = ragSources;

            ChatClient.Builder clientBuilder;
            if (systemPrompt != null) {
                clientBuilder = ChatClient.builder(chatModel)
                        .defaultSystem(systemPrompt)
                        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
            } else {
                clientBuilder = chatClient.mutate();
            }

            // Send sessionId first
            emitter.send(SseEmitter.event()
                    .name("session")
                    .data("{\"sessionId\":\"" + sessionId + "\"}"));

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

    private RagContext buildRagContext(String message, Long knowledgeBaseId) {
        String systemPrompt = null;
        Long tenantId = TenantContext.getTenantId();

        var kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> BusinessException.notFound("知识库"));
        if (tenantId != null && !kb.getTenantId().equals(tenantId)) {
            throw BusinessException.forbidden("无权访问此知识库");
        }
        if (kb.getSystemPrompt() != null && !kb.getSystemPrompt().isEmpty()) {
            systemPrompt = kb.getSystemPrompt();
        }

        // 使用参数化表达式避免注入风险（值为Long类型，但仍做防御性处理）
        String safeKbId = String.valueOf(knowledgeBaseId);
        String filterExpression = tenantId != null
                ? "knowledge_base_id == '%s' && tenant_id == '%s'".formatted(
                    safeKbId.replace("'", ""), String.valueOf(tenantId).replace("'", ""))
                : "knowledge_base_id == '%s'".formatted(safeKbId.replace("'", ""));
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(message)
                        .topK(topK)
                        .similarityThreshold(threshold)
                        .filterExpression(filterExpression)
                        .build()
        );

        List<Document> filteredDocs = (docs != null) ? docs.stream()
                .filter(doc -> {
                    Object kbId = doc.getMetadata().get("knowledge_base_id");
                    return kbId != null && kbId.toString().equals(knowledgeBaseId.toString());
                })
                .collect(Collectors.toList()) : Collections.emptyList();

        String context = filteredDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        String augmentedPrompt = context.isEmpty()
                ? message
                : "根据以下参考信息回答问题：\n\n" + context + "\n\n问题：" + message;

        List<ChatResponse.SourceReference> sources = filteredDocs.stream()
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

        return new RagContext(augmentedPrompt, systemPrompt, sources);
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
        Conversation conversation = Conversation.builder()
                .sessionId(UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .tenantId(tenantId)
                .knowledgeBaseId(request.getKnowledgeBaseId())
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
}

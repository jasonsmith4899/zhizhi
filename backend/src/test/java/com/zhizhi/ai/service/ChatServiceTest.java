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
import com.zhizhi.ai.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatModel chatModel;
    @Mock
    private VectorStore vectorStore;
    @Mock
    private HybridRetrievalService hybridRetrievalService;
    @Mock
    private KnowledgeGraphService knowledgeGraphService;
    @Mock
    private DocumentTagRepository documentTagRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApiKeyRepository apiKeyRepository;
    @Mock
    private AuthUtil authUtil;
    @Mock
    private ChatMemory chatMemory;

    @InjectMocks
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
        setField(chatService, "topK", 5);
        setField(chatService, "threshold", 0.7);
        setField(chatService, "maxChunksPerDoc", 2);
        setField(chatService, "recallMinScore", 0.5);
        setField(chatService, "recallMinChunks", 1);
        setField(chatService, "maxContextMessages", 10);
        setField(chatService, "sseTimeout", 60000L);
        setField(chatService, "maxConversations", 50);
        setField(chatService, "maxMessages", 100);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
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

    private org.springframework.ai.chat.model.ChatResponse createAiChatResponse(String content) {
        return new org.springframework.ai.chat.model.ChatResponse(
                List.of(new Generation(new AssistantMessage(content)))
        );
    }

    /**
     * Sets up mocks for a complete successful chat flow (knowledgeBaseId mode).
     */
    private void mockSuccessfulChatFlow(String reply) {
        KnowledgeBase kb = KnowledgeBase.builder().id(1L).tenantId(1L).userId(1L).name("Test KB").build();
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(kb));

        Document doc = Document.builder()
                .text("test reference content")
                .metadata(Map.of("document_id", "1", "filename", "test.pdf", "chunk_index", "0"))
                .score(0.8)
                .build();
        when(hybridRetrievalService.hybridSearch(any(), any(), anyLong(), any()))
                .thenReturn(List.of(doc));

        when(knowledgeGraphService.querySubgraph(any(), any(), anyLong())).thenReturn(null);
        when(knowledgeGraphService.querySubgraphStructured(any(), any(), anyLong())).thenReturn(null);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            conv.setId(1L);
            return conv;
        });
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(1L);
            return msg;
        });

        when(chatMemory.get(anyString())).thenReturn(List.of());
        when(chatModel.call(any(Prompt.class))).thenReturn(createAiChatResponse(reply));
    }

    // ==================== chat: happy path ====================

    @Test
    void chat_happyPath_returnsResponse() {
        mockSuccessfulChatFlow("test reply");

        ChatRequest request = new ChatRequest();
        request.setMessage("test question");
        request.setKnowledgeBaseId(1L);

        ChatResponse response = chatService.chat(request, 1L, null);

        assertThat(response.getReply()).isEqualTo("test reply");
        assertThat(response.getSessionId()).isNotNull();
        assertThat(response.getSources()).isNotEmpty();
        assertThat(response.getSources().get(0).getContent()).isEqualTo("test reference content");
        assertThat(response.getMessageId()).isNotNull();
    }

    @Test
    void chat_withApiKeyId_usesApiKeyConfig() {
        KnowledgeBase kb = KnowledgeBase.builder().id(1L).tenantId(1L).name("Test KB").build();
        ApiKey apiKey = ApiKey.builder()
                .id(10L)
                .assistantPersona("You are a helpful assistant")
                .merchantBackground("Test background")
                .answerRules("Be concise")
                .knowledgeBases(new HashSet<>(Set.of(kb)))
                .build();
        when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(apiKey));

        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(kb));

        Document doc = Document.builder()
                .text("ref content")
                .metadata(Map.of("document_id", "1", "filename", "doc.pdf", "chunk_index", "0"))
                .score(0.8)
                .build();
        when(hybridRetrievalService.hybridSearch(any(), any(), anyLong(), any())).thenReturn(List.of(doc));
        when(knowledgeGraphService.querySubgraph(any(), any(), anyLong())).thenReturn(null);
        when(knowledgeGraphService.querySubgraphStructured(any(), any(), anyLong())).thenReturn(null);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        when(chatMemory.get(anyString())).thenReturn(List.of());
        when(chatModel.call(any(Prompt.class))).thenReturn(createAiChatResponse("api key reply"));

        ChatRequest request = new ChatRequest();
        request.setMessage("test question");
        request.setApiKeyId(10L);

        ChatResponse response = chatService.chat(request, 1L, null);

        assertThat(response.getReply()).isEqualTo("api key reply");
        verify(apiKeyRepository, atLeast(1)).findById(10L);
    }

    @Test
    void chat_withExistingSession_usesExistingConversation() {
        Conversation existingConv = Conversation.builder()
                .id(1L).sessionId("existing-session").userId(1L).tenantId(1L).messageCount(4).build();
        when(conversationRepository.findByTenantIdAndSessionId(1L, "existing-session"))
                .thenReturn(Optional.of(existingConv));

        KnowledgeBase kb = KnowledgeBase.builder().id(1L).tenantId(1L).name("Test KB").build();
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(kb));

        Document doc = Document.builder()
                .text("content")
                .metadata(Map.of("document_id", "1", "filename", "doc.pdf", "chunk_index", "0"))
                .score(0.8)
                .build();
        when(hybridRetrievalService.hybridSearch(any(), any(), anyLong(), any())).thenReturn(List.of(doc));
        when(knowledgeGraphService.querySubgraph(any(), any(), anyLong())).thenReturn(null);
        when(knowledgeGraphService.querySubgraphStructured(any(), any(), anyLong())).thenReturn(null);

        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        when(chatMemory.get(anyString())).thenReturn(List.of());
        when(chatModel.call(any(Prompt.class))).thenReturn(createAiChatResponse("session reply"));

        ChatRequest request = new ChatRequest();
        request.setMessage("continuation");
        request.setKnowledgeBaseId(1L);
        request.setSessionId("existing-session");

        ChatResponse response = chatService.chat(request, 1L, null);

        assertThat(response.getReply()).isEqualTo("session reply");
        assertThat(response.getSessionId()).isEqualTo("existing-session");
        // Existing conversation used, messageCount updated from 4 to 6
        verify(conversationRepository).save(argThat(c -> c.getId().equals(1L) && c.getMessageCount() == 6));
    }

    // ==================== chat: validation errors ====================

    @Test
    void chat_noApiKeyIdOrKnowledgeBaseId_throwsBadRequest() {
        ChatRequest request = new ChatRequest();
        request.setMessage("test");

        assertThatThrownBy(() -> chatService.chat(request, 1L, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(400));
    }

    @Test
    void chat_apiKeyNotFound_throwsNotFound() {
        when(apiKeyRepository.findById(999L)).thenReturn(Optional.empty());

        ChatRequest request = new ChatRequest();
        request.setMessage("test");
        request.setApiKeyId(999L);

        assertThatThrownBy(() -> chatService.chat(request, 1L, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(404));
    }

    @Test
    void chat_apiKeyWithNoKnowledgeBases_throwsBadRequest() {
        ApiKey apiKey = ApiKey.builder()
                .id(10L)
                .knowledgeBases(new HashSet<>())
                .build();
        when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(apiKey));

        ChatRequest request = new ChatRequest();
        request.setMessage("test");
        request.setApiKeyId(10L);

        assertThatThrownBy(() -> chatService.chat(request, 1L, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(400));
    }

    @Test
    void chat_knowledgeBaseNotFound_throwsNotFound() {
        when(knowledgeBaseRepository.findById(999L)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatRequest request = new ChatRequest();
        request.setMessage("test");
        request.setKnowledgeBaseId(999L);

        assertThatThrownBy(() -> chatService.chat(request, 1L, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(404));
    }

    @Test
    void chat_tenantMismatch_throwsForbidden() {
        KnowledgeBase kb = KnowledgeBase.builder().id(1L).tenantId(2L).name("Other Tenant KB").build();
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(kb));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatRequest request = new ChatRequest();
        request.setMessage("test");
        request.setKnowledgeBaseId(1L);

        assertThatThrownBy(() -> chatService.chat(request, 1L, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(403));
    }

    @Test
    void chat_existingSessionNotFound_throwsNotFound() {
        when(conversationRepository.findByTenantIdAndSessionId(1L, "nonexistent"))
                .thenReturn(Optional.empty());

        ChatRequest request = new ChatRequest();
        request.setMessage("test");
        request.setKnowledgeBaseId(1L);
        request.setSessionId("nonexistent");

        assertThatThrownBy(() -> chatService.chat(request, 1L, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(404));
    }

    // ==================== chat: recall quality ====================

    @Test
    void chat_recallNotSatisfied_politeRejection() {
        KnowledgeBase kb = KnowledgeBase.builder().id(1L).tenantId(1L).name("Test KB").build();
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(kb));

        // Low score -> recall not satisfied
        Document lowScoreDoc = Document.builder()
                .text("low relevance")
                .metadata(Map.of("document_id", "1", "filename", "doc.pdf", "chunk_index", "0"))
                .score(0.2)
                .build();
        when(hybridRetrievalService.hybridSearch(any(), any(), anyLong(), any()))
                .thenReturn(List.of(lowScoreDoc));
        when(knowledgeGraphService.querySubgraph(any(), any(), anyLong())).thenReturn(null);
        when(knowledgeGraphService.querySubgraphStructured(any(), any(), anyLong())).thenReturn(null);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        when(chatModel.call(any(Prompt.class))).thenReturn(createAiChatResponse("polite rejection"));

        ChatRequest request = new ChatRequest();
        request.setMessage("unknown topic");
        request.setKnowledgeBaseId(1L);

        ChatResponse response = chatService.chat(request, 1L, null);

        assertThat(response.getReply()).isEqualTo("polite rejection");
        assertThat(response.getSources()).isEmpty();
    }

    @Test
    void chat_withNoRecallAndNoKag_politeRejection() {
        KnowledgeBase kb = KnowledgeBase.builder().id(1L).tenantId(1L).name("Test KB").build();
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(kb));

        // No results at all
        when(hybridRetrievalService.hybridSearch(any(), any(), anyLong(), any()))
                .thenReturn(Collections.emptyList());
        when(knowledgeGraphService.querySubgraph(any(), any(), anyLong())).thenReturn(null);
        when(knowledgeGraphService.querySubgraphStructured(any(), any(), anyLong())).thenReturn(null);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        when(chatModel.call(any(Prompt.class))).thenReturn(createAiChatResponse("cannot answer"));

        ChatRequest request = new ChatRequest();
        request.setMessage("completely unrelated");
        request.setKnowledgeBaseId(1L);

        ChatResponse response = chatService.chat(request, 1L, null);

        assertThat(response.getReply()).isEqualTo("cannot answer");
        assertThat(response.getSources()).isEmpty();
    }

    // ==================== chat: KAG integration ====================

    @Test
    void chat_withKagSubgraph_usesAugmentedPrompt() {
        KnowledgeBase kb = KnowledgeBase.builder().id(1L).tenantId(1L).name("Test KB").build();
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(kb));

        // Low recall
        Document lowScoreDoc = Document.builder()
                .text("low relevance")
                .metadata(Map.of("document_id", "1", "filename", "doc.pdf", "chunk_index", "0"))
                .score(0.2)
                .build();
        when(hybridRetrievalService.hybridSearch(any(), any(), anyLong(), any()))
                .thenReturn(List.of(lowScoreDoc));

        // But KAG subgraph found
        when(knowledgeGraphService.querySubgraph(any(), any(), anyLong()))
                .thenReturn("EntityA —属于→ EntityB");
        when(knowledgeGraphService.querySubgraphStructured(any(), any(), anyLong()))
                .thenReturn(List.of(Map.of("sourceName", "EntityA", "predicate", "属于", "targetName", "EntityB")));

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        when(chatMemory.get(anyString())).thenReturn(List.of());
        when(chatModel.call(any(Prompt.class))).thenReturn(createAiChatResponse("kag enhanced reply"));

        ChatRequest request = new ChatRequest();
        request.setMessage("test question");
        request.setKnowledgeBaseId(1L);

        ChatResponse response = chatService.chat(request, 1L, null);

        assertThat(response.getReply()).isEqualTo("kag enhanced reply");
        // Sources empty because recall not satisfied (KAG doesn't produce sources)
        assertThat(response.getSources()).isEmpty();
    }

    // ==================== chat: tag filtering ====================

    @Test
    void chat_withTagIds_passesAllowedDocIds() {
        KnowledgeBase kb = KnowledgeBase.builder().id(1L).tenantId(1L).name("Test KB").build();
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(kb));
        when(documentTagRepository.findDocumentIdsByTagIds(eq(1L), eq(List.of(10L, 20L))))
                .thenReturn(List.of(1L, 2L));

        Document doc = Document.builder()
                .text("tagged content")
                .metadata(Map.of("document_id", "1", "filename", "doc.pdf", "chunk_index", "0"))
                .score(0.8)
                .build();
        when(hybridRetrievalService.hybridSearch(any(), any(), anyLong(), any())).thenReturn(List.of(doc));
        when(knowledgeGraphService.querySubgraph(any(), any(), anyLong())).thenReturn(null);
        when(knowledgeGraphService.querySubgraphStructured(any(), any(), anyLong())).thenReturn(null);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        when(chatMemory.get(anyString())).thenReturn(List.of());
        when(chatModel.call(any(Prompt.class))).thenReturn(createAiChatResponse("tagged reply"));

        ChatRequest request = new ChatRequest();
        request.setMessage("test question");
        request.setKnowledgeBaseId(1L);
        request.setTagIds(List.of(10L, 20L));

        ChatResponse response = chatService.chat(request, 1L, null);

        assertThat(response.getReply()).isEqualTo("tagged reply");
        // Verify the allowedDocIds from tag filtering were passed to hybrid search
        verify(hybridRetrievalService).hybridSearch(
                eq("test question"),
                eq(Set.of(1L)),
                eq(1L),
                eq(Set.of(1L, 2L))
        );
    }

    // ==================== listConversations ====================

    @Test
    void listConversations_withTenantId_returnsList() {
        List<Conversation> conversations = List.of(
                Conversation.builder().id(1L).sessionId("abc").userId(1L).tenantId(1L).build(),
                Conversation.builder().id(2L).sessionId("def").userId(1L).tenantId(1L).build()
        );
        Page<Conversation> page = new PageImpl<>(conversations);
        when(conversationRepository.findByTenantIdOrderByUpdatedAtDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(page);

        List<Conversation> result = chatService.listConversations(1L);

        assertThat(result).hasSize(2);
        verify(conversationRepository).findByTenantIdOrderByUpdatedAtDesc(eq(1L), any(PageRequest.class));
    }

    @Test
    void listConversations_nullTenantId_queriesByUserId() {
        TenantContext.clear();
        List<Conversation> conversations = List.of(
                Conversation.builder().id(1L).sessionId("abc").userId(1L).build()
        );
        Page<Conversation> page = new PageImpl<>(conversations);
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(page);

        List<Conversation> result = chatService.listConversations(1L);

        assertThat(result).hasSize(1);
        verify(conversationRepository).findByUserIdOrderByUpdatedAtDesc(eq(1L), any(PageRequest.class));
    }

    // ==================== getMessages ====================

    @Test
    void getMessages_happyPath_returnsMessages() {
        Conversation conv = Conversation.builder().id(1L).sessionId("abc").userId(1L).tenantId(1L).build();
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conv));

        List<Message> messages = List.of(
                Message.builder().id(1L).conversationId(1L).tenantId(1L).role("user").content("hello").build(),
                Message.builder().id(2L).conversationId(1L).tenantId(1L).role("assistant").content("hi").build()
        );
        Page<Message> page = new PageImpl<>(messages);
        when(messageRepository.findByTenantIdAndConversationIdOrderByCreatedAtAsc(eq(1L), eq(1L), any(PageRequest.class)))
                .thenReturn(page);

        List<Message> result = chatService.getMessages(1L, 1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo("user");
        assertThat(result.get(1).getRole()).isEqualTo("assistant");
    }

    @Test
    void getMessages_conversationNotFound_throwsNotFound() {
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getMessages(999L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(404));
    }

    @Test
    void getMessages_wrongUser_throwsForbidden() {
        Conversation conv = Conversation.builder().id(1L).sessionId("abc").userId(2L).tenantId(1L).build();
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> chatService.getMessages(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(403));
    }

    // ==================== deleteConversation ====================

    @Test
    void deleteConversation_happyPath_deletes() {
        Conversation conv = Conversation.builder().id(1L).sessionId("abc").userId(1L).tenantId(1L).build();
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conv));

        chatService.deleteConversation(1L, 1L);

        verify(conversationRepository).delete(conv);
    }

    @Test
    void deleteConversation_wrongUser_throwsForbidden() {
        Conversation conv = Conversation.builder().id(1L).sessionId("abc").userId(2L).tenantId(1L).build();
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> chatService.deleteConversation(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(403));
    }

    @Test
    void deleteConversation_tenantMismatch_throwsForbidden() {
        Conversation conv = Conversation.builder().id(1L).sessionId("abc").userId(1L).tenantId(2L).build();
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> chatService.deleteConversation(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(403));
    }

    // ==================== streamChat ====================

    @Test
    void streamChat_recallNotSatisfied_returnsEmitter() {
        KnowledgeBase kb = KnowledgeBase.builder().id(1L).tenantId(1L).name("Test KB").build();
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(kb));

        Document lowScoreDoc = Document.builder()
                .text("low score")
                .metadata(Map.of("document_id", "1", "filename", "doc.pdf", "chunk_index", "0"))
                .score(0.1)
                .build();
        when(hybridRetrievalService.hybridSearch(any(), any(), anyLong(), any()))
                .thenReturn(List.of(lowScoreDoc));
        when(knowledgeGraphService.querySubgraph(any(), any(), anyLong())).thenReturn(null);
        when(knowledgeGraphService.querySubgraphStructured(any(), any(), anyLong())).thenReturn(null);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        when(chatModel.call(any(Prompt.class))).thenReturn(createAiChatResponse("rejection"));

        ChatRequest request = new ChatRequest();
        request.setMessage("unknown topic");
        request.setKnowledgeBaseId(1L);

        SseEmitter emitter = chatService.streamChat(request, 1L, null);

        assertThat(emitter).isNotNull();
        // User message + assistant message saved
        verify(messageRepository, times(2)).save(any(Message.class));
        verify(conversationRepository, atLeastOnce()).save(any(Conversation.class));
    }

    @Test
    void streamChat_noApiKeyIdOrKnowledgeBaseId_throwsBadRequest() {
        ChatRequest request = new ChatRequest();
        request.setMessage("test");

        assertThatThrownBy(() -> chatService.streamChat(request, 1L, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(400));
    }
}

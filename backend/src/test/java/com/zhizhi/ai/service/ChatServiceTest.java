package com.zhizhi.ai.service;

import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.model.dto.ChatRequest;
import com.zhizhi.ai.model.dto.ChatResponse;
import com.zhizhi.ai.model.entity.Conversation;
import com.zhizhi.ai.model.entity.Message;
import com.zhizhi.ai.repository.ConversationRepository;
import com.zhizhi.ai.repository.KnowledgeBaseRepository;
import com.zhizhi.ai.repository.MessageRepository;
import com.zhizhi.ai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 单元测试")
class ChatServiceTest {

    private ChatService chatService;

    @Mock
    private ChatModel chatModel;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMemory chatMemory;

    private final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                chatModel, vectorStore, conversationRepository,
                knowledgeBaseRepository, messageRepository, userRepository, chatMemory);
        ReflectionTestUtils.setField(chatService, "topK", 5);
        ReflectionTestUtils.setField(chatService, "threshold", 0.7);
        ReflectionTestUtils.setField(chatService, "maxContextMessages", 10);
    }

    @Nested
    @DisplayName("会话管理")
    class ConversationManagement {

        @Test
        @DisplayName("获取会话列表")
        void listConversations() {
            Conversation conv = Conversation.builder()
                    .id(1L).sessionId("abc123").userId(USER_ID).title("测试对话").build();
            Page<Conversation> page = new PageImpl<>(List.of(conv));

            when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(eq(USER_ID), any(PageRequest.class)))
                    .thenReturn(page);

            List<Conversation> result = chatService.listConversations(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("测试对话");
        }

        @Test
        @DisplayName("获取会话消息")
        void getMessages() {
            Conversation conv = Conversation.builder()
                    .id(1L).sessionId("abc123").userId(USER_ID).build();
            Message msg = Message.builder()
                    .id(1L).conversationId(1L).role("user").content("你好").build();
            Page<Message> page = new PageImpl<>(List.of(msg));

            when(conversationRepository.findById(1L)).thenReturn(Optional.of(conv));
            when(messageRepository.findByConversationIdOrderByCreatedAtAsc(eq(1L), any(PageRequest.class)))
                    .thenReturn(page);

            List<Message> result = chatService.getMessages(1L, USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContent()).isEqualTo("你好");
        }

        @Test
        @DisplayName("无权访问他人会话")
        void getMessages_forbidden() {
            Conversation conv = Conversation.builder()
                    .id(1L).userId(999L).build();

            when(conversationRepository.findById(1L)).thenReturn(Optional.of(conv));

            assertThatThrownBy(() -> chatService.getMessages(1L, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(403));
        }

        @Test
        @DisplayName("删除会话")
        void deleteConversation() {
            Conversation conv = Conversation.builder()
                    .id(1L).userId(USER_ID).build();

            when(conversationRepository.findById(1L)).thenReturn(Optional.of(conv));

            chatService.deleteConversation(1L, USER_ID);

            verify(conversationRepository).delete(conv);
        }

        @Test
        @DisplayName("无权删除他人会话")
        void deleteConversation_forbidden() {
            Conversation conv = Conversation.builder()
                    .id(1L).userId(999L).build();

            when(conversationRepository.findById(1L)).thenReturn(Optional.of(conv));

            assertThatThrownBy(() -> chatService.deleteConversation(1L, USER_ID))
                    .isInstanceOf(BusinessException.class);
        }
    }
}

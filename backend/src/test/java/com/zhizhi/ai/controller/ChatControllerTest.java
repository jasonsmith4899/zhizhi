package com.zhizhi.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.GlobalExceptionHandler;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.config.SecurityConfig;
import com.zhizhi.ai.model.dto.ChatRequest;
import com.zhizhi.ai.model.dto.ChatResponse;
import com.zhizhi.ai.model.entity.Conversation;
import com.zhizhi.ai.model.entity.Message;
import com.zhizhi.ai.repository.ApiKeyRepository;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.UserRepository;
import com.zhizhi.ai.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@ActiveProfiles("test")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private AuthUtil authUtil;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private ApiKeyRepository apiKeyRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TenantMemberRepository tenantMemberRepository;

    @BeforeEach
    void setUp() {
        when(authUtil.getUserId(any(Authentication.class))).thenReturn(1L);
    }

    // ========== chat ==========

    @Test
    @WithMockUser
    void chat_success() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");
        request.setKnowledgeBaseId(10L);

        ChatResponse response = ChatResponse.builder()
                .reply("Hi there!")
                .sessionId("session-123")
                .messageId("msg-456")
                .sources(List.of())
                .build();

        when(chatService.chat(any(ChatRequest.class), eq(1L), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.reply").value("Hi there!"))
                .andExpect(jsonPath("$.data.sessionId").value("session-123"))
                .andExpect(jsonPath("$.data.messageId").value("msg-456"));
    }

    @Test
    @WithMockUser
    void chat_blankMessage_returns400() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("");
        request.setKnowledgeBaseId(10L);

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser
    void chat_serviceThrowsBusinessException_propagates() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");
        request.setKnowledgeBaseId(10L);

        when(chatService.chat(any(), eq(1L), any()))
                .thenThrow(BusinessException.quotaExceeded("配额已用完"));

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(429))
                .andExpect(jsonPath("$.code").value(429));
    }

    @Test
    @WithMockUser
    void chat_withSourceReferences_returnsSources() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("What is AI?");
        request.setKnowledgeBaseId(10L);

        ChatResponse.SourceReference source = ChatResponse.SourceReference.builder()
                .documentId(1L).documentName("AI Guide.pdf")
                .content("AI is artificial intelligence...")
                .score(0.95).chunkIndex(0)
                .build();

        ChatResponse response = ChatResponse.builder()
                .reply("AI stands for artificial intelligence.")
                .sessionId("session-123")
                .messageId("msg-789")
                .sources(List.of(source))
                .build();

        when(chatService.chat(any(), eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sources").isArray())
                .andExpect(jsonPath("$.data.sources[0].documentName").value("AI Guide.pdf"))
                .andExpect(jsonPath("$.data.sources[0].score").value(0.95));
    }

    // ========== conversations ==========

    @Test
    @WithMockUser
    void conversations_success() throws Exception {
        Conversation conv = Conversation.builder()
                .id(1L).title("Test Chat").sessionId("sess-1")
                .channel("web").messageCount(4)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(chatService.listConversations(1L)).thenReturn(List.of(conv));

        mockMvc.perform(get("/api/v1/chat/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("Test Chat"))
                .andExpect(jsonPath("$.data[0].sessionId").value("sess-1"))
                .andExpect(jsonPath("$.data[0].channel").value("web"));
    }

    @Test
    @WithMockUser
    void conversations_empty_returnsEmptyArray() throws Exception {
        when(chatService.listConversations(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/chat/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== messages ==========

    @Test
    @WithMockUser
    void messages_success() throws Exception {
        Message msg = Message.builder()
                .id(1L).conversationId(1L).tenantId(1L)
                .role("user").content("Hello")
                .createdAt(LocalDateTime.now())
                .build();

        when(chatService.getMessages(1L, 1L)).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/v1/chat/conversations/1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].role").value("user"))
                .andExpect(jsonPath("$.data[0].content").value("Hello"));
    }

    @Test
    @WithMockUser
    void messages_notFound_returns404() throws Exception {
        when(chatService.getMessages(99L, 1L))
                .thenThrow(BusinessException.notFound("会话"));

        mockMvc.perform(get("/api/v1/chat/conversations/99/messages"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ========== deleteConversation ==========

    @Test
    @WithMockUser
    void deleteConversation_success() throws Exception {
        doNothing().when(chatService).deleteConversation(1L, 1L);

        mockMvc.perform(delete("/api/v1/chat/conversations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(chatService).deleteConversation(1L, 1L);
    }

    @Test
    @WithMockUser
    void deleteConversation_notFound_returns404() throws Exception {
        doThrow(BusinessException.notFound("会话")).when(chatService).deleteConversation(99L, 1L);

        mockMvc.perform(delete("/api/v1/chat/conversations/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}

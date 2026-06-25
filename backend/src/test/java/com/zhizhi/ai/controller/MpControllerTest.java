package com.zhizhi.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.BusinessException;
import com.zhizhi.ai.common.GlobalExceptionHandler;
import com.zhizhi.ai.common.JwtUtil;
import com.zhizhi.ai.config.SecurityConfig;
import com.zhizhi.ai.model.dto.ChatRequest;
import com.zhizhi.ai.model.dto.ChatResponse;
import com.zhizhi.ai.model.dto.KnowledgeBaseDTO;
import com.zhizhi.ai.model.entity.Conversation;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import com.zhizhi.ai.model.entity.Message;
import com.zhizhi.ai.repository.ApiKeyRepository;
import com.zhizhi.ai.repository.TenantMemberRepository;
import com.zhizhi.ai.repository.UserRepository;
import com.zhizhi.ai.service.ChatService;
import com.zhizhi.ai.service.KnowledgeService;
import com.zhizhi.ai.service.StatsService;
import com.zhizhi.ai.service.WechatService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MpController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@ActiveProfiles("test")
class MpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WechatService wechatService;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private KnowledgeService knowledgeService;

    @MockitoBean
    private StatsService statsService;

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

    // ========== login ==========

    @Test
    void login_success() throws Exception {
        Map<String, Object> responseData = Map.of(
                "accessToken", "mp-access-token",
                "refreshToken", "mp-refresh-token",
                "isNewUser", false
        );
        when(wechatService.miniProgramLogin("wx-code-123", null)).thenReturn(responseData);

        mockMvc.perform(post("/api/mp/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"wx-code-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("mp-access-token"))
                .andExpect(jsonPath("$.data.isNewUser").value(false));
    }

    @Test
    void login_withTenantId_passesTenantId() throws Exception {
        Map<String, Object> responseData = Map.of(
                "accessToken", "mp-access-token",
                "refreshToken", "mp-refresh-token"
        );
        when(wechatService.miniProgramLogin("wx-code-456", 5L)).thenReturn(responseData);

        mockMvc.perform(post("/api/mp/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"wx-code-456\",\"tenantId\":\"5\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void login_invalidCode_propagatesException() throws Exception {
        when(wechatService.miniProgramLogin("bad-code", null))
                .thenThrow(BusinessException.badRequest("微信登录失败"));

        mockMvc.perform(post("/api/mp/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"bad-code\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ========== knowledgeBases ==========

    @Test
    @WithMockUser
    void knowledgeBases_success() throws Exception {
        KnowledgeBase kb = KnowledgeBase.builder()
                .id(1L).userId(1L).tenantId(1L)
                .name("小程序知识库").status("active")
                .createdAt(LocalDateTime.now())
                .build();
        when(knowledgeService.listByUser(1L)).thenReturn(List.of(kb));

        mockMvc.perform(get("/api/mp/knowledge-bases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("小程序知识库"));
    }

    @Test
    @WithMockUser
    void knowledgeBases_empty_returnsEmptyArray() throws Exception {
        when(knowledgeService.listByUser(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/mp/knowledge-bases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== chat ==========

    @Test
    @WithMockUser
    void chat_success() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("你好");
        request.setKnowledgeBaseId(10L);

        ChatResponse response = ChatResponse.builder()
                .reply("你好！有什么可以帮你的？")
                .sessionId("mp-session-1")
                .messageId("mp-msg-1")
                .sources(List.of())
                .build();

        when(chatService.chat(any(ChatRequest.class), eq(1L), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/mp/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.reply").value("你好！有什么可以帮你的？"))
                .andExpect(jsonPath("$.data.sessionId").value("mp-session-1"));
    }

    @Test
    @WithMockUser
    void chat_blankMessage_returns400() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("");
        request.setKnowledgeBaseId(10L);

        mockMvc.perform(post("/api/mp/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ========== conversations ==========

    @Test
    @WithMockUser
    void conversations_success() throws Exception {
        Conversation conv = Conversation.builder()
                .id(1L).title("小程序会话").sessionId("mp-sess-1")
                .channel("miniprogram").messageCount(2)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(chatService.listConversations(1L)).thenReturn(List.of(conv));

        mockMvc.perform(get("/api/mp/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].channel").value("miniprogram"));
    }

    // ========== messages ==========

    @Test
    @WithMockUser
    void messages_success() throws Exception {
        Message msg = Message.builder()
                .id(1L).conversationId(1L).tenantId(1L)
                .role("assistant").content("你好！")
                .createdAt(LocalDateTime.now())
                .build();
        when(chatService.getMessages(1L, 1L)).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/mp/conversations/1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].role").value("assistant"))
                .andExpect(jsonPath("$.data[0].content").value("你好！"));
    }

    @Test
    @WithMockUser
    void messages_notFound_returns404() throws Exception {
        when(chatService.getMessages(99L, 1L))
                .thenThrow(BusinessException.notFound("会话"));

        mockMvc.perform(get("/api/mp/conversations/99/messages"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ========== deleteConversation ==========

    @Test
    @WithMockUser
    void deleteConversation_success() throws Exception {
        doNothing().when(chatService).deleteConversation(1L, 1L);

        mockMvc.perform(delete("/api/mp/conversations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(chatService).deleteConversation(1L, 1L);
    }

    @Test
    @WithMockUser
    void deleteConversation_notFound_returns404() throws Exception {
        doThrow(BusinessException.notFound("会话")).when(chatService).deleteConversation(99L, 1L);

        mockMvc.perform(delete("/api/mp/conversations/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}

package com.zhizhi.ai.controller;

import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.Result;
import com.zhizhi.ai.model.dto.ChatRequest;
import com.zhizhi.ai.model.dto.ChatResponse;
import com.zhizhi.ai.model.dto.ConversationDTO;
import com.zhizhi.ai.model.dto.KnowledgeBaseDTO;
import com.zhizhi.ai.model.dto.MessageDTO;
import com.zhizhi.ai.service.ChatService;
import com.zhizhi.ai.service.KnowledgeService;
import com.zhizhi.ai.service.StatsService;
import com.zhizhi.ai.service.WechatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 微信小程序专用API
 */
@RestController
@RequestMapping("/api/mp")
@RequiredArgsConstructor
public class MpController {

    private final WechatService wechatService;
    private final ChatService chatService;
    private final KnowledgeService knowledgeService;
    private final StatsService statsService;
    private final AuthUtil authUtil;

    /**
     * 微信小程序登录（无需认证）
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        Long tenantId = request.containsKey("tenantId") ?
                Long.valueOf(request.get("tenantId")) : null;
        return Result.ok(wechatService.miniProgramLogin(code, tenantId));
    }

    /**
     * 获取当前租户的知识库列表（小程序端）
     */
    @GetMapping("/knowledge-bases")
    public Result<List<KnowledgeBaseDTO>> knowledgeBases(Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        List<KnowledgeBaseDTO> dtos = knowledgeService.listByUser(userId)
                .stream()
                .map(KnowledgeBaseDTO::fromEntity)
                .toList();
        return Result.ok(dtos);
    }

    /**
     * 小程序端对话
     */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request,
                                      Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        return Result.ok(chatService.chat(request, userId, authentication));
    }

    /**
     * 小程序端会话列表
     */
    @GetMapping("/conversations")
    public Result<List<ConversationDTO>> conversations(Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        List<ConversationDTO> dtos = chatService.listConversations(userId)
                .stream()
                .map(ConversationDTO::fromEntity)
                .toList();
        return Result.ok(dtos);
    }

    /**
     * 小程序端会话消息
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public Result<List<MessageDTO>> messages(@PathVariable Long conversationId,
                                           Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        List<MessageDTO> dtos = chatService.getMessages(conversationId, userId)
                .stream()
                .map(MessageDTO::fromEntity)
                .toList();
        return Result.ok(dtos);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversations/{conversationId}")
    public Result<Void> deleteConversation(@PathVariable Long conversationId,
                                            Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        chatService.deleteConversation(conversationId, userId);
        return Result.ok();
    }
}

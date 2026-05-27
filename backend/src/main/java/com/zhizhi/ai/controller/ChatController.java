package com.zhizhi.ai.controller;

import com.zhizhi.ai.common.AuthUtil;
import com.zhizhi.ai.common.RateLimit;
import com.zhizhi.ai.common.Result;
import com.zhizhi.ai.model.dto.ChatRequest;
import com.zhizhi.ai.model.dto.ChatResponse;
import com.zhizhi.ai.model.dto.MessageDTO;
import com.zhizhi.ai.model.dto.ConversationDTO;
import com.zhizhi.ai.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AuthUtil authUtil;

    @PostMapping
    @RateLimit(maxRequests = 10, windowSeconds = 60)
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request,
                                      Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        return Result.ok(chatService.chat(request, userId));
    }

    @PostMapping("/stream")
    @RateLimit(maxRequests = 10, windowSeconds = 60)
    public SseEmitter stream(@Valid @RequestBody ChatRequest request,
                              Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        return chatService.streamChat(request, userId);
    }

    @GetMapping("/conversations")
    public Result<List<ConversationDTO>> conversations(Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        List<ConversationDTO> dtos = chatService.listConversations(userId)
                .stream()
                .map(ConversationDTO::fromEntity)
                .toList();
        return Result.ok(dtos);
    }

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

    @DeleteMapping("/conversations/{conversationId}")
    public Result<Void> deleteConversation(@PathVariable Long conversationId,
                                            Authentication authentication) {
        Long userId = authUtil.getUserId(authentication);
        chatService.deleteConversation(conversationId, userId);
        return Result.ok();
    }
}

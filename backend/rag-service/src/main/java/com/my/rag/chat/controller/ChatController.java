package com.my.rag.chat.controller;

import com.my.rag.api.chat.dto.ChatLogDetailResponse;
import com.my.rag.api.chat.dto.ChatLogSummaryResponse;
import com.my.rag.api.chat.dto.ChatRequest;
import com.my.rag.api.chat.dto.ChatResponse;
import com.my.rag.chat.service.ChatService;
import com.my.rag.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("API request: POST /api/rag/chat, documentIds: {}, topK: {}",
                request.documentIds(), request.topK());
        return ApiResponse.success(chatService.chat(request));
    }

    @GetMapping("/logs")
    public ApiResponse<List<ChatLogSummaryResponse>> listChatLogs() {
        log.info("API request: GET /api/rag/chat/logs");
        return ApiResponse.success(chatService.listChatLogs());
    }

    @GetMapping("/logs/{id}")
    public ApiResponse<ChatLogDetailResponse> getChatLogDetail(@PathVariable Long id) {
        log.info("API request: GET /api/rag/chat/logs/{}", id);
        return ApiResponse.success(chatService.getChatLogDetail(id));
    }
}

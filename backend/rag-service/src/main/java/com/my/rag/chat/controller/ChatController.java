package com.my.rag.chat.controller;

import com.my.rag.api.chat.dto.ChatRequest;
import com.my.rag.api.chat.dto.ChatResponse;
import com.my.rag.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/chat")
public class ChatController {

    @PostMapping
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.success(new ChatResponse("当前资料中没有找到明确依据。", true, List.of()));
    }
}


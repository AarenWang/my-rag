package com.my.rag.chat.dto;

public record LlmChatResponse(
        String content, String finishReason, Integer promptTokens, Integer completionTokens) {}


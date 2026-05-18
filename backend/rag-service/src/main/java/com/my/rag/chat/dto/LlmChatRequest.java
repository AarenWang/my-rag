package com.my.rag.chat.dto;

import java.util.List;

public record LlmChatRequest(String model, List<LlmMessage> messages, Double temperature) {}


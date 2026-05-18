package com.my.rag.chat.client;

import com.my.rag.chat.dto.LlmChatRequest;
import com.my.rag.chat.dto.LlmChatResponse;

public interface LlmClient {

    LlmChatResponse chat(LlmChatRequest request);
}

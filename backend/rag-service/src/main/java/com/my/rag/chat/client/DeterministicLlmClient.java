package com.my.rag.chat.client;

import com.my.rag.chat.dto.LlmChatRequest;
import com.my.rag.chat.dto.LlmChatResponse;

class DeterministicLlmClient implements LlmClient {

    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        return new LlmChatResponse(
                "基于检索到的资料，已找到可用于回答问题的相关片段。当前使用 mock LLM，接入真实 LLM 后会生成更自然的总结。",
                "stop",
                null,
                null);
    }
}

package com.my.rag.chat.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.rag.chat.dto.LlmChatRequest;
import com.my.rag.chat.dto.LlmChatResponse;
import com.my.rag.config.RagProperties;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ConfigurableLlmClient implements LlmClient {

    private final RagProperties ragProperties;
    private final LlmClient mockClient;
    private final LlmClient openAiClient;

    public ConfigurableLlmClient(RagProperties ragProperties, ObjectMapper objectMapper) {
        this.ragProperties = ragProperties;
        this.mockClient = new DeterministicLlmClient();
        this.openAiClient = new OpenAiCompatibleLlmClient(ragProperties, objectMapper);
    }

    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        String provider = ragProperties.getModel().getChatProvider();
        return switch ((provider == null ? "mock" : provider).toLowerCase(Locale.ROOT)) {
            case "mock", "deterministic", "local" -> mockClient.chat(request);
            case "openai", "openai-compatible", "compatible", "dashscope", "aliyun", "deepseek" ->
                    openAiClient.chat(request);
            default -> throw new LlmClientException("Unsupported chat provider: " + provider);
        };
    }
}

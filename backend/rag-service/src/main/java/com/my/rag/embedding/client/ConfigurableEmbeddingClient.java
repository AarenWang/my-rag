package com.my.rag.embedding.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.rag.config.RagProperties;
import com.my.rag.embedding.dto.EmbeddingRequest;
import com.my.rag.embedding.dto.EmbeddingResponse;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ConfigurableEmbeddingClient implements EmbeddingClient {

    private final RagProperties ragProperties;
    private final EmbeddingClient mockClient;
    private final EmbeddingClient openAiClient;

    public ConfigurableEmbeddingClient(RagProperties ragProperties, ObjectMapper objectMapper) {
        this.ragProperties = ragProperties;
        this.mockClient = new DeterministicEmbeddingClient(ragProperties);
        this.openAiClient = new OpenAiCompatibleEmbeddingClient(ragProperties, objectMapper);
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        String provider = ragProperties.getModel().getEmbeddingProvider();
        return switch ((provider == null ? "mock" : provider).toLowerCase(Locale.ROOT)) {
            case "mock", "deterministic", "local" -> mockClient.embed(request);
            case "openai", "openai-compatible", "compatible", "dashscope", "aliyun" -> openAiClient.embed(request);
            default -> throw new EmbeddingClientException("Unsupported embedding provider: " + provider);
        };
    }
}

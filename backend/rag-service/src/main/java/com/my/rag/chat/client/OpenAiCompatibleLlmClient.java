package com.my.rag.chat.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.rag.chat.dto.LlmChatRequest;
import com.my.rag.chat.dto.LlmChatResponse;
import com.my.rag.chat.dto.LlmMessage;
import com.my.rag.config.RagProperties;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.util.StringUtils;

class OpenAiCompatibleLlmClient implements LlmClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    OpenAiCompatibleLlmClient(RagProperties ragProperties, ObjectMapper objectMapper) {
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
        int timeoutSeconds = Math.max(1, ragProperties.getModel().getChatRequestTimeoutSeconds());
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds))
                .writeTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        String apiKey = ragProperties.getModel().getChatApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new LlmClientException("RAG_CHAT_API_KEY is required for OpenAI-compatible chat");
        }

        try {
            Request httpRequest = new Request.Builder()
                    .url(endpoint())
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(objectMapper.writeValueAsBytes(payload(request)), JSON))
                    .build();
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String body = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    throw new LlmClientException("Chat API failed, status: " + response.code() + ", body: " + body);
                }
                return parseResponse(body);
            }
        } catch (IOException e) {
            throw new LlmClientException("Failed to call chat API", e);
        }
    }

    private String endpoint() {
        String baseUrl = ragProperties.getModel().getChatBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new LlmClientException("Chat base URL must not be empty");
        }
        return baseUrl.replaceAll("/+$", "") + "/chat/completions";
    }

    private Map<String, Object> payload(LlmChatRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", request.model());
        payload.put("messages", request.messages().stream().map(this::messagePayload).toList());
        if (request.temperature() != null) {
            payload.put("temperature", request.temperature());
        }
        return payload;
    }

    private Map<String, String> messagePayload(LlmMessage message) {
        Map<String, String> result = new HashMap<>();
        result.put("role", message.role());
        result.put("content", message.content());
        return result;
    }

    private LlmChatResponse parseResponse(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode choice = root.path("choices").isArray() && !root.path("choices").isEmpty()
                ? root.path("choices").get(0)
                : null;
        if (choice == null) {
            throw new LlmClientException("Chat API response choices must contain at least one item");
        }

        String content = choice.path("message").path("content").asText("");
        if (!StringUtils.hasText(content)) {
            throw new LlmClientException("Chat API returned empty content");
        }

        String finishReason = choice.path("finish_reason").asText(null);
        JsonNode usage = root.path("usage");
        Integer promptTokens = usage.path("prompt_tokens").isNumber() ? usage.path("prompt_tokens").asInt() : null;
        Integer completionTokens = usage.path("completion_tokens").isNumber()
                ? usage.path("completion_tokens").asInt()
                : null;
        return new LlmChatResponse(content, finishReason, promptTokens, completionTokens);
    }
}

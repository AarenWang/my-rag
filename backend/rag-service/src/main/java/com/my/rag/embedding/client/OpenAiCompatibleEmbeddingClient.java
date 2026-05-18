package com.my.rag.embedding.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.rag.config.RagProperties;
import com.my.rag.embedding.dto.EmbeddingRequest;
import com.my.rag.embedding.dto.EmbeddingResponse;
import com.my.rag.embedding.dto.EmbeddingVector;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.util.StringUtils;

class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    OpenAiCompatibleEmbeddingClient(RagProperties ragProperties, ObjectMapper objectMapper) {
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
        int timeoutSeconds = Math.max(1, ragProperties.getModel().getEmbeddingRequestTimeoutSeconds());
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds))
                .writeTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        String apiKey = ragProperties.getModel().getEmbeddingApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new EmbeddingClientException("RAG_EMBEDDING_API_KEY is required for OpenAI-compatible embedding");
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
                    throw new EmbeddingClientException("Embedding API failed, status: "
                            + response.code() + ", body: " + body);
                }
                return parseResponse(request, body);
            }
        } catch (IOException e) {
            throw new EmbeddingClientException("Failed to call embedding API", e);
        }
    }

    private String endpoint() {
        String baseUrl = ragProperties.getModel().getEmbeddingBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new EmbeddingClientException("Embedding base URL must not be empty");
        }
        return baseUrl.replaceAll("/+$", "") + "/embeddings";
    }

    private Map<String, Object> payload(EmbeddingRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", request.model());
        payload.put("input", request.inputs().stream().map(input -> input.text()).toList());
        int dimension = ragProperties.getModel().getEmbeddingDimension();
        if (dimension > 0) {
            payload.put("dimensions", dimension);
        }
        return payload;
    }

    private EmbeddingResponse parseResponse(EmbeddingRequest request, String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.path("data");
        if (!data.isArray()) {
            throw new EmbeddingClientException("Embedding API response data must be an array");
        }

        List<EmbeddingVector> vectors = new ArrayList<>();
        for (JsonNode item : data) {
            int index = item.path("index").asInt(vectors.size());
            if (index < 0 || index >= request.inputs().size()) {
                throw new EmbeddingClientException("Embedding API returned invalid index: " + index);
            }
            JsonNode embedding = item.path("embedding");
            if (!embedding.isArray() || embedding.isEmpty()) {
                throw new EmbeddingClientException("Embedding API returned empty vector at index: " + index);
            }
            List<Double> vector = new ArrayList<>(embedding.size());
            for (JsonNode value : embedding) {
                vector.add(value.asDouble());
            }
            vectors.add(new EmbeddingVector(request.inputs().get(index).chunkId(), vector));
        }

        int dimension = vectors.isEmpty() ? 0 : vectors.get(0).vector().size();
        return new EmbeddingResponse(request.model(), dimension, vectors);
    }
}

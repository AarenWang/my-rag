package com.my.rag.retrieval.service;

import com.my.rag.config.RagProperties;
import com.my.rag.embedding.client.EmbeddingClient;
import com.my.rag.embedding.client.EmbeddingClientException;
import com.my.rag.embedding.dto.EmbeddingInput;
import com.my.rag.embedding.dto.EmbeddingRequest;
import com.my.rag.embedding.dto.EmbeddingResponse;
import com.my.rag.embedding.dto.EmbeddingVector;
import com.my.rag.retrieval.dto.RetrievalQuery;
import com.my.rag.retrieval.dto.RetrievalResult;
import com.my.rag.retrieval.dto.RetrievedChunk;
import com.my.rag.retrieval.repository.RetrievalMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RetrievalService {

    private static final Long QUESTION_INPUT_ID = 0L;

    private final RagProperties ragProperties;
    private final EmbeddingClient embeddingClient;
    private final RetrievalMapper retrievalMapper;

    public RetrievalService(
            RagProperties ragProperties,
            EmbeddingClient embeddingClient,
            RetrievalMapper retrievalMapper) {
        this.ragProperties = ragProperties;
        this.embeddingClient = embeddingClient;
        this.retrievalMapper = retrievalMapper;
    }

    public RetrievalResult retrieve(RetrievalQuery query) {
        if (query == null || !StringUtils.hasText(query.question())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question must not be blank");
        }

        String model = ragProperties.getModel().getEmbeddingModel();
        if (!StringUtils.hasText(model)) {
            throw new EmbeddingClientException("Embedding model must not be empty");
        }

        int topK = resolveTopK(query.topK());
        double scoreThreshold = resolveScoreThreshold(query.scoreThreshold());
        List<Double> questionEmbedding = query.questionEmbedding() == null || query.questionEmbedding().isEmpty()
                ? embedQuestion(model, query.question())
                : query.questionEmbedding();

        List<RetrievedChunk> chunks = retrievalMapper.search(
                toPgVector(questionEmbedding),
                model,
                normalizeDocumentIds(query.documentIds()),
                topK,
                scoreThreshold);

        return new RetrievalResult(query.question(), chunks.isEmpty(), chunks);
    }

    private List<Double> embedQuestion(String model, String question) {
        EmbeddingResponse response = embeddingClient.embed(new EmbeddingRequest(
                model,
                List.of(new EmbeddingInput(QUESTION_INPUT_ID, question))));
        if (response == null || response.vectors() == null || response.vectors().size() != 1) {
            throw new EmbeddingClientException("Question embedding response must contain exactly one vector");
        }

        EmbeddingVector vector = response.vectors().get(0);
        if (vector == null || vector.vector() == null || vector.vector().isEmpty()) {
            throw new EmbeddingClientException("Question embedding vector is empty");
        }

        int expectedDimension = ragProperties.getModel().getEmbeddingDimension();
        if (expectedDimension > 0 && vector.vector().size() != expectedDimension) {
            throw new EmbeddingClientException("Question embedding dimension mismatch, expected: "
                    + expectedDimension + ", actual: " + vector.vector().size());
        }
        return vector.vector();
    }

    private int resolveTopK(int requestedTopK) {
        int defaultTopK = Math.max(1, ragProperties.getRetrieval().getDefaultTopK());
        return requestedTopK > 0 ? requestedTopK : defaultTopK;
    }

    private double resolveScoreThreshold(double requestedScoreThreshold) {
        return requestedScoreThreshold;
    }

    private List<Long> normalizeDocumentIds(List<Long> documentIds) {
        if (documentIds == null) {
            return List.of();
        }
        return documentIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }

    private String toPgVector(List<Double> vector) {
        StringBuilder builder = new StringBuilder(vector.size() * 10);
        builder.append('[');
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            Double value = vector.get(i);
            if (value == null || !Double.isFinite(value)) {
                throw new EmbeddingClientException("Question embedding vector contains invalid value");
            }
            builder.append(String.format(Locale.ROOT, "%.9f", value));
        }
        builder.append(']');
        return builder.toString();
    }
}

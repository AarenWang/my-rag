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
import com.my.rag.retrieval.rerank.RerankerClient;
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
    private final KeywordRetrievalService keywordRetrievalService;
    private final HybridRanker hybridRanker;
    private final RerankerClient rerankerClient;

    public RetrievalService(
            RagProperties ragProperties,
            EmbeddingClient embeddingClient,
            RetrievalMapper retrievalMapper,
            KeywordRetrievalService keywordRetrievalService,
            HybridRanker hybridRanker,
            RerankerClient rerankerClient) {
        this.ragProperties = ragProperties;
        this.embeddingClient = embeddingClient;
        this.retrievalMapper = retrievalMapper;
        this.keywordRetrievalService = keywordRetrievalService;
        this.hybridRanker = hybridRanker;
        this.rerankerClient = rerankerClient;
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
        int vectorTopK = resolveVectorTopK(query.topK());
        int keywordTopK = resolveKeywordTopK(query.topK());
        int rrfTopK = resolveRrfTopK(topK);
        int rerankTopK = resolveRerankTopK(topK);
        double scoreThreshold = resolveScoreThreshold(query.scoreThreshold());
        List<Long> documentIds = normalizeDocumentIds(query.documentIds());
        List<Double> questionEmbedding = query.questionEmbedding() == null || query.questionEmbedding().isEmpty()
                ? embedQuestion(model, query.question())
                : query.questionEmbedding();

        List<RetrievedChunk> vectorChunks = retrievalMapper.search(
                toPgVector(questionEmbedding),
                model,
                documentIds,
                vectorTopK,
                scoreThreshold);
        List<RetrievedChunk> chunks = isHybridMode()
                ? rerankerClient.rerank(
                                query.question(),
                                hybridRanker.mergeByRrf(
                                        vectorChunks,
                                        keywordRetrievalService.search(query.question(), documentIds, keywordTopK),
                                        ragProperties.getRetrieval().getRrfK(),
                                        rrfTopK),
                                rerankTopK)
                        .stream()
                        .map(candidate -> candidate.toRetrievedChunk())
                        .limit(topK)
                        .toList()
                : asVectorOnlyChunks(vectorChunks, topK);

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

    private int resolveVectorTopK(int requestedTopK) {
        if (requestedTopK > 0) {
            return requestedTopK;
        }
        int vectorTopK = ragProperties.getRetrieval().getVectorTopK();
        return vectorTopK > 0 ? vectorTopK : resolveTopK(requestedTopK);
    }

    private int resolveKeywordTopK(int requestedTopK) {
        if (requestedTopK > 0) {
            return requestedTopK;
        }
        int keywordTopK = ragProperties.getRetrieval().getKeywordTopK();
        return keywordTopK > 0 ? keywordTopK : resolveTopK(requestedTopK);
    }

    private int resolveRrfTopK(int fallbackTopK) {
        int rrfTopK = ragProperties.getRetrieval().getRrfTopK();
        return rrfTopK > 0 ? rrfTopK : fallbackTopK;
    }

    private int resolveRerankTopK(int fallbackTopK) {
        int rerankTopK = ragProperties.getRetrieval().getRerankTopK();
        return rerankTopK > 0 ? rerankTopK : fallbackTopK;
    }

    private double resolveScoreThreshold(double requestedScoreThreshold) {
        return requestedScoreThreshold;
    }

    private boolean isHybridMode() {
        return "hybrid".equalsIgnoreCase(ragProperties.getRetrieval().getMode());
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

    private List<RetrievedChunk> asVectorOnlyChunks(List<RetrievedChunk> chunks, int topK) {
        java.util.ArrayList<RetrievedChunk> decorated = new java.util.ArrayList<>();
        for (int i = 0; i < chunks.size() && decorated.size() < topK; i++) {
            decorated.add(asVectorOnlyChunk(chunks.get(i), i + 1));
        }
        return List.copyOf(decorated);
    }

    private RetrievedChunk asVectorOnlyChunk(RetrievedChunk chunk, int rank) {
        return new RetrievedChunk(
                chunk.documentId(),
                chunk.documentTitle(),
                chunk.chapterTitle(),
                chunk.chunkId(),
                chunk.chunkIndex(),
                chunk.startParagraph(),
                chunk.endParagraph(),
                chunk.content(),
                chunk.score(),
                chunk.score(),
                null,
                null,
                null,
                chunk.score(),
                rank,
                null,
                List.of("vector"));
    }
}

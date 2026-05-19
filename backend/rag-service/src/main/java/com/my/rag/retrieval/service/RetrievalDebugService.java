package com.my.rag.retrieval.service;

import com.my.rag.api.retrieval.dto.RetrievalDebugRequest;
import com.my.rag.api.retrieval.dto.RetrievalDebugResponse;
import com.my.rag.chat.dto.Evidence;
import com.my.rag.chat.dto.EvidencePack;
import com.my.rag.chat.service.ContextBuilder;
import com.my.rag.config.RagProperties;
import com.my.rag.document.service.DocumentScopeResolver;
import com.my.rag.embedding.client.EmbeddingClient;
import com.my.rag.embedding.client.EmbeddingClientException;
import com.my.rag.embedding.dto.EmbeddingInput;
import com.my.rag.embedding.dto.EmbeddingRequest;
import com.my.rag.embedding.dto.EmbeddingResponse;
import com.my.rag.embedding.dto.EmbeddingVector;
import com.my.rag.retrieval.dto.HybridCandidate;
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
public class RetrievalDebugService {

    private static final Long QUESTION_INPUT_ID = 0L;

    private final RagProperties ragProperties;
    private final EmbeddingClient embeddingClient;
    private final RetrievalMapper retrievalMapper;
    private final KeywordQueryService keywordQueryService;
    private final KeywordRetrievalService keywordRetrievalService;
    private final HybridRanker hybridRanker;
    private final RerankerClient rerankerClient;
    private final ContextBuilder contextBuilder;
    private final DocumentScopeResolver documentScopeResolver;

    public RetrievalDebugService(
            RagProperties ragProperties,
            EmbeddingClient embeddingClient,
            RetrievalMapper retrievalMapper,
            KeywordQueryService keywordQueryService,
            KeywordRetrievalService keywordRetrievalService,
            HybridRanker hybridRanker,
            RerankerClient rerankerClient,
            ContextBuilder contextBuilder,
            DocumentScopeResolver documentScopeResolver) {
        this.ragProperties = ragProperties;
        this.embeddingClient = embeddingClient;
        this.retrievalMapper = retrievalMapper;
        this.keywordQueryService = keywordQueryService;
        this.keywordRetrievalService = keywordRetrievalService;
        this.hybridRanker = hybridRanker;
        this.rerankerClient = rerankerClient;
        this.contextBuilder = contextBuilder;
        this.documentScopeResolver = documentScopeResolver;
    }

    public RetrievalDebugResponse debug(RetrievalDebugRequest request) {
        if (request == null || !StringUtils.hasText(request.question())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question must not be blank");
        }

        String model = ragProperties.getModel().getEmbeddingModel();
        if (!StringUtils.hasText(model)) {
            throw new EmbeddingClientException("Embedding model must not be empty");
        }

        int topK = resolveTopK(request.topK());
        int vectorTopK = resolveVectorTopK(request.topK());
        int keywordTopK = resolveKeywordTopK(request.topK());
        int rrfTopK = resolveRrfTopK(topK);
        int rerankTopK = resolveRerankTopK(topK);
        double scoreThreshold = resolveScoreThreshold(request.scoreThreshold());
        List<Long> documentIds = documentScopeResolver.resolveDocumentIds(
                request.documentIds(),
                request.collectionIds()
        );

        List<Double> questionEmbedding = embedQuestion(model, request.question());
        List<RetrievedChunk> vectorChunks = retrievalMapper.search(
                toPgVector(questionEmbedding),
                model,
                documentIds,
                vectorTopK,
                scoreThreshold);

        List<String> keywordQueries = keywordQueryService.generate(request.question());
        List<RetrievedChunk> keywordChunks = keywordRetrievalService.search(request.question(), documentIds, keywordTopK);

        List<HybridCandidate> rrfCandidates = List.of();
        List<HybridCandidate> rerankedCandidates = List.of();
        List<RetrievedChunk> finalChunks = asVectorOnlyChunks(vectorChunks, topK);

        if (isHybridMode()) {
            rrfCandidates = hybridRanker.mergeByRrf(
                    vectorChunks,
                    keywordChunks,
                    ragProperties.getRetrieval().getRrfK(),
                    rrfTopK);
            rerankedCandidates = rerankerClient.rerank(request.question(), rrfCandidates, rerankTopK);
            finalChunks = rerankedCandidates.stream()
                    .map(HybridCandidate::toRetrievedChunk)
                    .limit(topK)
                    .toList();
        }

        EvidencePack evidencePack = contextBuilder.build(request.question(), finalChunks);
        return new RetrievalDebugResponse(
                request.question(),
                ragProperties.getRetrieval().getMode(),
                ragProperties.getRetrieval().isKeywordIndexEnabled(),
                keywordQueries,
                toConfig(topK, vectorTopK, keywordTopK, rrfTopK, rerankTopK, scoreThreshold),
                toChunkCandidates(vectorChunks),
                toChunkCandidates(keywordChunks),
                toHybridCandidates(rrfCandidates),
                toHybridCandidates(rerankedCandidates),
                toEvidences(evidencePack));
    }

    private RetrievalDebugResponse.Config toConfig(
            int topK,
            int vectorTopK,
            int keywordTopK,
            int rrfTopK,
            int rerankTopK,
            double scoreThreshold) {
        return new RetrievalDebugResponse.Config(
                topK,
                vectorTopK,
                keywordTopK,
                rrfTopK,
                rerankTopK,
                Math.max(1, ragProperties.getRetrieval().getContextTopK()),
                scoreThreshold,
                Math.max(1, ragProperties.getRetrieval().getRrfK()),
                Math.max(500, ragProperties.getRetrieval().getMaxContextChars()),
                ragProperties.getRetrieval().getRerankerProvider());
    }

    private List<RetrievalDebugResponse.Candidate> toChunkCandidates(List<RetrievedChunk> chunks) {
        return java.util.stream.IntStream.range(0, chunks.size())
                .mapToObj(index -> toCandidate(index + 1, chunks.get(index)))
                .toList();
    }

    private List<RetrievalDebugResponse.Candidate> toHybridCandidates(List<HybridCandidate> candidates) {
        return java.util.stream.IntStream.range(0, candidates.size())
                .mapToObj(index -> toCandidate(index + 1, candidates.get(index).toRetrievedChunk()))
                .toList();
    }

    private RetrievalDebugResponse.Candidate toCandidate(int rank, RetrievedChunk chunk) {
        return new RetrievalDebugResponse.Candidate(
                rank,
                chunk.documentId(),
                chunk.documentTitle(),
                chunk.chapterTitle(),
                chunk.chunkId(),
                chunk.chunkIndex(),
                chunk.startParagraph(),
                chunk.endParagraph(),
                chunk.content(),
                chunk.score(),
                chunk.vectorScore(),
                chunk.keywordScore(),
                chunk.rrfScore(),
                chunk.rerankScore(),
                chunk.finalScore(),
                chunk.vectorRank(),
                chunk.keywordRank(),
                chunk.retrievalSources());
    }

    private List<RetrievalDebugResponse.Evidence> toEvidences(EvidencePack evidencePack) {
        return evidencePack.evidences().stream()
                .map(this::toEvidence)
                .toList();
    }

    private RetrievalDebugResponse.Evidence toEvidence(Evidence evidence) {
        RetrievedChunk chunk = evidence.chunk();
        return new RetrievalDebugResponse.Evidence(
                evidence.sourceId(),
                chunk.documentId(),
                chunk.documentTitle(),
                chunk.chapterTitle(),
                chunk.chunkId(),
                chunk.chunkIndex(),
                evidence.content(),
                chunk.finalScore(),
                chunk.retrievalSources());
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

    private int resolveTopK(Integer requestedTopK) {
        int defaultTopK = Math.max(1, ragProperties.getRetrieval().getDefaultTopK());
        return requestedTopK != null && requestedTopK > 0 ? requestedTopK : defaultTopK;
    }

    private int resolveVectorTopK(Integer requestedTopK) {
        if (requestedTopK != null && requestedTopK > 0) {
            return requestedTopK;
        }
        int vectorTopK = ragProperties.getRetrieval().getVectorTopK();
        return vectorTopK > 0 ? vectorTopK : resolveTopK(requestedTopK);
    }

    private int resolveKeywordTopK(Integer requestedTopK) {
        if (requestedTopK != null && requestedTopK > 0) {
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

    private double resolveScoreThreshold(Double requestedScoreThreshold) {
        return requestedScoreThreshold != null
                ? requestedScoreThreshold
                : ragProperties.getRetrieval().getScoreThreshold();
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

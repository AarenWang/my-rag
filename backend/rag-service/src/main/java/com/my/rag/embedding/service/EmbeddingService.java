package com.my.rag.embedding.service;

import com.my.rag.chunk.entity.RagDocumentChunk;
import com.my.rag.chunk.service.ChunkService;
import com.my.rag.config.RagProperties;
import com.my.rag.embedding.client.EmbeddingClient;
import com.my.rag.embedding.client.EmbeddingClientException;
import com.my.rag.embedding.dto.EmbeddingInput;
import com.my.rag.embedding.dto.EmbeddingRequest;
import com.my.rag.embedding.dto.EmbeddingResponse;
import com.my.rag.embedding.dto.EmbeddingVector;
import com.my.rag.embedding.entity.RagChunkEmbedding;
import com.my.rag.embedding.repository.RagChunkEmbeddingMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final ChunkService chunkService;
    private final EmbeddingClient embeddingClient;
    private final RagChunkEmbeddingMapper embeddingMapper;
    private final RagProperties ragProperties;

    public EmbeddingService(
            ChunkService chunkService,
            EmbeddingClient embeddingClient,
            RagChunkEmbeddingMapper embeddingMapper,
            RagProperties ragProperties) {
        this.chunkService = chunkService;
        this.embeddingClient = embeddingClient;
        this.embeddingMapper = embeddingMapper;
        this.ragProperties = ragProperties;
    }

    @Transactional
    public int embedDocumentChunks(Long documentId) {
        List<RagDocumentChunk> chunks = chunkService.getChunksByDocumentId(documentId);
        if (chunks.isEmpty()) {
            throw new EmbeddingClientException("Cannot generate embeddings for an empty chunk list: " + documentId);
        }

        String model = ragProperties.getModel().getEmbeddingModel();
        if (!StringUtils.hasText(model)) {
            throw new EmbeddingClientException("Embedding model must not be empty");
        }

        int batchSize = Math.max(1, ragProperties.getModel().getEmbeddingBatchSize());
        int expectedDimension = ragProperties.getModel().getEmbeddingDimension();
        int savedCount = 0;
        log.info("Generating embeddings, documentId: {}, chunks: {}, batchSize: {}, model: {}",
                documentId, chunks.size(), batchSize, model);

        embeddingMapper.deleteByDocumentId(documentId);

        for (int start = 0; start < chunks.size(); start += batchSize) {
            List<RagDocumentChunk> batch = chunks.subList(start, Math.min(start + batchSize, chunks.size()));
            EmbeddingResponse response = embeddingClient.embed(toRequest(model, batch));
            validateResponse(response, batch, expectedDimension);
            Map<Long, EmbeddingVector> vectorsByChunkId = toVectorMap(response.vectors());
            for (RagDocumentChunk chunk : batch) {
                EmbeddingVector vector = vectorsByChunkId.get(chunk.getId());
                if (vector == null) {
                    throw new EmbeddingClientException("Embedding response missing chunkId: " + chunk.getId());
                }
                RagChunkEmbedding embedding = new RagChunkEmbedding();
                embedding.setChunkId(chunk.getId());
                embedding.setEmbedding(toPgVector(vector.vector()));
                embedding.setEmbeddingModel(model);
                embeddingMapper.upsertEmbedding(embedding);
                savedCount++;
            }
            log.info("Embedding batch saved, documentId: {}, saved: {}/{}", documentId, savedCount, chunks.size());
        }

        log.info("Embeddings generated successfully, documentId: {}, total: {}", documentId, savedCount);
        return savedCount;
    }

    public void deleteByDocumentId(Long documentId) {
        embeddingMapper.deleteByDocumentId(documentId);
    }

    private static final int MAX_EMBEDDING_LENGTH_CHARS = 8000;

    private EmbeddingRequest toRequest(String model, List<RagDocumentChunk> chunks) {
        List<EmbeddingInput> inputs = chunks.stream()
                .map(chunk -> {
                    String content = chunk.getContent();
                    if (content != null && content.length() > MAX_EMBEDDING_LENGTH_CHARS) {
                        log.warn("Chunk content exceeds max length, truncating, chunkId: {}, originalLength: {}",
                                chunk.getId(), content.length());
                        content = content.substring(0, MAX_EMBEDDING_LENGTH_CHARS);
                    }
                    return new EmbeddingInput(chunk.getId(), content);
                })
                .toList();
        return new EmbeddingRequest(model, inputs);
    }

    private void validateResponse(EmbeddingResponse response, List<RagDocumentChunk> batch, int expectedDimension) {
        if (response == null || response.vectors() == null || response.vectors().isEmpty()) {
            throw new EmbeddingClientException("Embedding response is empty");
        }
        if (response.vectors().size() != batch.size()) {
            throw new EmbeddingClientException("Embedding response size mismatch, expected: "
                    + batch.size() + ", actual: " + response.vectors().size());
        }
        for (EmbeddingVector vector : response.vectors()) {
            validateVector(vector, expectedDimension);
        }
    }

    private void validateVector(EmbeddingVector vector, int expectedDimension) {
        if (vector == null || vector.chunkId() == null) {
            throw new EmbeddingClientException("Embedding vector chunkId is missing");
        }
        if (vector.vector() == null || vector.vector().isEmpty()) {
            throw new EmbeddingClientException("Embedding vector is empty, chunkId: " + vector.chunkId());
        }
        if (expectedDimension > 0 && vector.vector().size() != expectedDimension) {
            throw new EmbeddingClientException("Embedding dimension mismatch, chunkId: "
                    + vector.chunkId() + ", expected: " + expectedDimension + ", actual: " + vector.vector().size());
        }
        for (Double value : vector.vector()) {
            if (value == null || !Double.isFinite(value)) {
                throw new EmbeddingClientException("Embedding vector contains invalid value, chunkId: " + vector.chunkId());
            }
        }
    }

    private Map<Long, EmbeddingVector> toVectorMap(List<EmbeddingVector> vectors) {
        Map<Long, EmbeddingVector> result = new HashMap<>();
        for (EmbeddingVector vector : vectors) {
            if (result.put(vector.chunkId(), vector) != null) {
                throw new EmbeddingClientException("Embedding response contains duplicate chunkId: " + vector.chunkId());
            }
        }
        return result;
    }

    private String toPgVector(List<Double> vector) {
        StringBuilder builder = new StringBuilder(vector.size() * 10);
        builder.append('[');
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.ROOT, "%.9f", vector.get(i)));
        }
        builder.append(']');
        return builder.toString();
    }
}

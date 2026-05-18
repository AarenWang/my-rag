package com.my.rag.embedding.client;

import com.my.rag.config.RagProperties;
import com.my.rag.embedding.dto.EmbeddingInput;
import com.my.rag.embedding.dto.EmbeddingRequest;
import com.my.rag.embedding.dto.EmbeddingResponse;
import com.my.rag.embedding.dto.EmbeddingVector;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

class DeterministicEmbeddingClient implements EmbeddingClient {

    private final RagProperties ragProperties;

    DeterministicEmbeddingClient(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        int dimension = ragProperties.getModel().getEmbeddingDimension();
        List<EmbeddingVector> vectors = request.inputs().stream()
                .map(input -> new EmbeddingVector(input.chunkId(), vectorize(input, dimension)))
                .toList();
        return new EmbeddingResponse(request.model(), dimension, vectors);
    }

    private List<Double> vectorize(EmbeddingInput input, int dimension) {
        double[] values = new double[dimension];
        byte[] bytes = input.text() == null ? new byte[0] : input.text().getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0) {
            throw new EmbeddingClientException("Embedding input is empty, chunkId: " + input.chunkId());
        }

        for (int i = 0; i < bytes.length; i += 16) {
            byte[] digest = sha256(bytes, i, Math.min(16, bytes.length - i));
            int bucket = Math.floorMod(toInt(digest, 0), dimension);
            double sign = (digest[4] & 1) == 0 ? 1.0 : -1.0;
            values[bucket] += sign * (1.0 + ((digest[5] & 0xff) / 255.0));
        }

        normalize(values);
        List<Double> result = new ArrayList<>(dimension);
        for (double value : values) {
            result.add(value);
        }
        return result;
    }

    private byte[] sha256(byte[] bytes, int offset, int length) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes, offset, length);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new EmbeddingClientException("SHA-256 algorithm is unavailable", e);
        }
    }

    private int toInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xff) << 24)
                | ((bytes[offset + 1] & 0xff) << 16)
                | ((bytes[offset + 2] & 0xff) << 8)
                | (bytes[offset + 3] & 0xff);
    }

    private void normalize(double[] values) {
        double norm = 0.0;
        for (double value : values) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            values[0] = 1.0;
            return;
        }
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i] / norm;
        }
    }
}

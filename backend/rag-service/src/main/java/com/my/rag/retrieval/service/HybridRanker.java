package com.my.rag.retrieval.service;

import com.my.rag.retrieval.dto.HybridCandidate;
import com.my.rag.retrieval.dto.RetrievedChunk;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HybridRanker {

    public List<HybridCandidate> mergeByRrf(
            List<RetrievedChunk> vectorChunks,
            List<RetrievedChunk> keywordChunks,
            int rrfK,
            int limit) {
        Map<Long, HybridCandidate> candidates = new LinkedHashMap<>();

        for (int i = 0; i < safeList(vectorChunks).size(); i++) {
            RetrievedChunk chunk = vectorChunks.get(i);
            if (chunk.chunkId() == null) {
                continue;
            }
            candidates.computeIfAbsent(chunk.chunkId(), ignored -> new HybridCandidate(chunk))
                    .addVectorHit(i + 1, chunk.score());
        }

        for (int i = 0; i < safeList(keywordChunks).size(); i++) {
            RetrievedChunk chunk = keywordChunks.get(i);
            if (chunk.chunkId() == null) {
                continue;
            }
            candidates.computeIfAbsent(chunk.chunkId(), ignored -> new HybridCandidate(chunk))
                    .addKeywordHit(i + 1, chunk.score());
        }

        int safeRrfK = Math.max(1, rrfK);
        return candidates.values().stream()
                .peek(candidate -> candidate.calculateRrfScore(safeRrfK))
                .sorted(Comparator.comparingDouble((HybridCandidate candidate) ->
                        candidate.rrfScore() == null ? 0.0d : candidate.rrfScore()).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    private List<RetrievedChunk> safeList(List<RetrievedChunk> chunks) {
        return chunks == null ? List.of() : chunks;
    }
}

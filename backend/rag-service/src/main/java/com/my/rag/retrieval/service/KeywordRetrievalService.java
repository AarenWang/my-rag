package com.my.rag.retrieval.service;

import com.my.rag.config.RagProperties;
import com.my.rag.retrieval.dto.RetrievedChunk;
import com.my.rag.retrieval.repository.KeywordRetrievalMapper;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KeywordRetrievalService {

    private final RagProperties ragProperties;
    private final KeywordRetrievalMapper keywordRetrievalMapper;
    private final KeywordQueryService keywordQueryService;

    public KeywordRetrievalService(
            RagProperties ragProperties,
            KeywordRetrievalMapper keywordRetrievalMapper,
            KeywordQueryService keywordQueryService) {
        this.ragProperties = ragProperties;
        this.keywordRetrievalMapper = keywordRetrievalMapper;
        this.keywordQueryService = keywordQueryService;
    }

    public List<RetrievedChunk> search(String question, List<Long> documentIds, int topK) {
        if (!ragProperties.getRetrieval().isKeywordIndexEnabled() || !StringUtils.hasText(question)) {
            return List.of();
        }
        List<String> keywordQueries = keywordQueryService.generate(question);
        if (keywordQueries.isEmpty()) {
            return List.of();
        }

        Map<Long, KeywordHitCandidate> candidates = new LinkedHashMap<>();
        int safeTopK = Math.max(1, topK);
        int globalRank = 1;
        for (String keywordQuery : keywordQueries) {
            List<RetrievedChunk> hits = searchOneQuery(keywordQuery, documentIds, safeTopK);
            for (RetrievedChunk hit : hits) {
                if (hit.chunkId() == null) {
                    continue;
                }
                candidates.computeIfAbsent(hit.chunkId(), ignored -> new KeywordHitCandidate(hit))
                        .accept(globalRank, hit.score());
                globalRank++;
            }
        }

        return candidates.values().stream()
                .sorted(Comparator
                        .comparingInt(KeywordHitCandidate::bestRank)
                        .thenComparing(Comparator.comparingDouble(KeywordHitCandidate::bestScore).reversed()))
                .limit(safeTopK)
                .map(KeywordHitCandidate::toRetrievedChunk)
                .toList();
    }

    private List<RetrievedChunk> searchOneQuery(String keywordQuery, List<Long> documentIds, int topK) {
        String textSearchConfig = ragProperties.getRetrieval().getTextSearchConfig();
        String queryMode = ragProperties.getRetrieval().getKeywordQueryMode() == null
                ? "websearch"
                : ragProperties.getRetrieval().getKeywordQueryMode().toLowerCase();
        return switch (queryMode) {
            case "plain" -> keywordRetrievalMapper.searchPlain(keywordQuery, textSearchConfig, documentIds, topK);
            case "phrase" -> keywordRetrievalMapper.searchPhrase(keywordQuery, textSearchConfig, documentIds, topK);
            case "websearch" -> keywordRetrievalMapper.searchWebsearch(keywordQuery, textSearchConfig, documentIds, topK);
            default -> keywordRetrievalMapper.searchWebsearch(keywordQuery, textSearchConfig, documentIds, topK);
        };
    }

    private static class KeywordHitCandidate {

        private final RetrievedChunk chunk;
        private int bestRank = Integer.MAX_VALUE;
        private double bestScore = 0.0d;

        KeywordHitCandidate(RetrievedChunk chunk) {
            this.chunk = chunk;
        }

        void accept(int rank, Double score) {
            double safeScore = score == null ? 0.0d : score;
            if (rank < bestRank || (rank == bestRank && safeScore > bestScore)) {
                bestRank = rank;
                bestScore = safeScore;
            }
        }

        int bestRank() {
            return bestRank;
        }

        double bestScore() {
            return bestScore;
        }

        RetrievedChunk toRetrievedChunk() {
            return new RetrievedChunk(
                    chunk.documentId(),
                    chunk.documentTitle(),
                    chunk.chapterTitle(),
                    chunk.chunkId(),
                    chunk.chunkIndex(),
                    chunk.startParagraph(),
                    chunk.endParagraph(),
                    chunk.content(),
                    bestScore);
        }
    }
}

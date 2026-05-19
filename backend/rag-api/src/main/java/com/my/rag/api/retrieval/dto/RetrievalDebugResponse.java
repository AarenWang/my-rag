package com.my.rag.api.retrieval.dto;

import java.util.List;

public record RetrievalDebugResponse(
        String question,
        String mode,
        boolean keywordIndexEnabled,
        List<String> keywordQueries,
        Config config,
        List<Candidate> vectorCandidates,
        List<Candidate> keywordCandidates,
        List<Candidate> rrfCandidates,
        List<Candidate> rerankedCandidates,
        List<Evidence> evidences) {

    public record Config(
            int topK,
            int vectorTopK,
            int keywordTopK,
            int rrfTopK,
            int rerankTopK,
            int contextTopK,
            double scoreThreshold,
            int rrfK,
            int maxContextChars,
            String rerankerProvider) {}

    public record Candidate(
            int rank,
            Long documentId,
            String documentTitle,
            String chapterTitle,
            Long chunkId,
            Integer chunkIndex,
            Integer startParagraph,
            Integer endParagraph,
            String content,
            Double score,
            Double vectorScore,
            Double keywordScore,
            Double rrfScore,
            Double rerankScore,
            Double finalScore,
            Integer vectorRank,
            Integer keywordRank,
            List<String> retrievalSources) {}

    public record Evidence(
            String sourceId,
            Long documentId,
            String documentTitle,
            String chapterTitle,
            Long chunkId,
            Integer chunkIndex,
            String content,
            Double finalScore,
            List<String> retrievalSources) {}
}

package com.my.rag.retrieval.dto;

import java.util.List;

public record RetrievedChunk(
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
        List<String> retrievalSources) {

    public RetrievedChunk(
            Long documentId,
            String documentTitle,
            String chapterTitle,
            Long chunkId,
            Integer chunkIndex,
            Integer startParagraph,
            Integer endParagraph,
            String content,
            Double score) {
        this(
                documentId,
                documentTitle,
                chapterTitle,
                chunkId,
                chunkIndex,
                startParagraph,
                endParagraph,
                content,
                score,
                null,
                null,
                null,
                null,
                score,
                null,
                null,
                List.of());
    }
}

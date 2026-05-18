package com.my.rag.api.retrieval.dto;

public record RetrievalSourceResponse(
        Long documentId,
        String documentTitle,
        String chapterTitle,
        Long chunkId,
        Integer chunkIndex,
        Double score,
        String content) {}


package com.my.rag.retrieval.dto;

public record RetrievedChunk(
        Long documentId,
        String documentTitle,
        String chapterTitle,
        Long chunkId,
        Integer chunkIndex,
        Integer startParagraph,
        Integer endParagraph,
        String content,
        Double score) {}


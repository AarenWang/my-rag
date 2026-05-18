package com.my.rag.api.chunk.dto;

public record ChunkResponse(
        Long chunkId,
        Long documentId,
        String chapterTitle,
        Integer chunkIndex,
        Integer startParagraph,
        Integer endParagraph,
        String content,
        String contentPreview,
        Integer tokenCount) {}


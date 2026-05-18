package com.my.rag.api.chunk.dto;

public record ChunkMetadata(
        Long documentId,
        String documentTitle,
        String chapterTitle,
        Integer chunkIndex,
        Integer startParagraph,
        Integer endParagraph) {}


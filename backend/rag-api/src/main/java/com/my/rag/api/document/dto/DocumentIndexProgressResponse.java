package com.my.rag.api.document.dto;

public record DocumentIndexProgressResponse(
        Long documentId,
        String documentStatus,
        String taskStatus,
        String stage,
        int progressPercent,
        int chunkCount,
        String message,
        String errorMessage) {}

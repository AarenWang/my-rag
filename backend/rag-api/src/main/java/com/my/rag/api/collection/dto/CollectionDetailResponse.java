package com.my.rag.api.collection.dto;

import java.time.LocalDateTime;

public record CollectionDetailResponse(
        Long collectionId,
        String name,
        String description,
        String tags,
        Boolean archived,
        Integer documentCount,
        Integer readyDocumentCount,
        Integer chunkCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

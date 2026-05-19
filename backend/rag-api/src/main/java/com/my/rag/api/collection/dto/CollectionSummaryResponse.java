package com.my.rag.api.collection.dto;

public record CollectionSummaryResponse(
        Long collectionId,
        String name,
        String description,
        Boolean archived,
        Integer documentCount,
        Integer readyDocumentCount,
        Integer chunkCount
) {}

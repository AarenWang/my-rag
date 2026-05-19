package com.my.rag.api.collection.dto;

public record CreateCollectionRequest(
        String name,
        String description,
        String tags
) {}

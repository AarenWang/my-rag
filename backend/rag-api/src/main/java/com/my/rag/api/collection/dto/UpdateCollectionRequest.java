package com.my.rag.api.collection.dto;

public record UpdateCollectionRequest(
        String name,
        String description,
        String tags
) {}

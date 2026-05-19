package com.my.rag.api.collection.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCollectionRequest(
        @NotBlank(message = "collection name must not be blank") String name,
        String description,
        String tags
) {}

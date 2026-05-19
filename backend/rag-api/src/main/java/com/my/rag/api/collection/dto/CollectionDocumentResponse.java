package com.my.rag.api.collection.dto;

public record CollectionDocumentResponse(
        Long documentId,
        String title,
        String fileName,
        String status
) {}

package com.my.rag.api.document.dto;

public record DocumentSummaryResponse(
        Long documentId, String title, String fileName, String fileType, String status, Long collectionId) {}


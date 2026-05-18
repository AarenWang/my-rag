package com.my.rag.api.document.dto;

public record DocumentUploadResponse(
        Long documentId,
        String title,
        String fileName,
        String fileType,
        String status,
        boolean duplicate) {}


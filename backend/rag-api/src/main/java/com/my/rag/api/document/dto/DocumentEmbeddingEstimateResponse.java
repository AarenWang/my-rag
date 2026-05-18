package com.my.rag.api.document.dto;

import java.math.BigDecimal;

public record DocumentEmbeddingEstimateResponse(
        Long documentId,
        int chunkCount,
        long estimatedTokens,
        BigDecimal pricePer1kTokens,
        BigDecimal estimatedCostCny,
        String model,
        int dimension) {}

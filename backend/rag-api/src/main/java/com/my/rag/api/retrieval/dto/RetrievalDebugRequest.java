package com.my.rag.api.retrieval.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RetrievalDebugRequest(
        @NotBlank(message = "question must not be blank") String question,
        List<Long> documentIds,
        Integer topK,
        Double scoreThreshold) {}

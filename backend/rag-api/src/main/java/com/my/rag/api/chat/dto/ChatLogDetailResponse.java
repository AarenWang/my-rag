package com.my.rag.api.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatLogDetailResponse(
        Long id,
        String question,
        String answer,
        String documentIds,
        List<String> retrievedChunkIds,
        Integer topK,
        Double minScore,
        Long latencyMs,
        LocalDateTime createdAt
) {}

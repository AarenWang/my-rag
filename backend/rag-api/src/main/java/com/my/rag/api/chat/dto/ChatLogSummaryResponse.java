package com.my.rag.api.chat.dto;

import java.time.LocalDateTime;

public record ChatLogSummaryResponse(
        Long id,
        String question,
        String answerPreview,
        LocalDateTime createdAt
) {}

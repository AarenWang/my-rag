package com.my.rag.api.chat.dto;

import java.util.List;

public record ChatResponse(String answer, boolean noAnswer, List<Source> sources) {

    public record Source(
            Long documentId,
            String documentTitle,
            String chapterTitle,
            Long chunkId,
            Integer chunkIndex,
            Double score) {}
}


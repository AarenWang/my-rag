package com.my.rag.retrieval.dto;

import java.util.List;

public record RetrievalQuery(
        String question,
        List<Double> questionEmbedding,
        List<Long> documentIds,
        int topK,
        double scoreThreshold) {}


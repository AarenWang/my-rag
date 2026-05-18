package com.my.rag.embedding.dto;

import java.util.List;

public record EmbeddingResponse(String model, int dimension, List<EmbeddingVector> vectors) {}


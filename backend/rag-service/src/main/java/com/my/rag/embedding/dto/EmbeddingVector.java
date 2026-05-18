package com.my.rag.embedding.dto;

import java.util.List;

public record EmbeddingVector(Long chunkId, List<Double> vector) {}


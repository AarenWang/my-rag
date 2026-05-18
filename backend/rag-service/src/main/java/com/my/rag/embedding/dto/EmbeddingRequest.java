package com.my.rag.embedding.dto;

import java.util.List;

public record EmbeddingRequest(String model, List<EmbeddingInput> inputs) {}


package com.my.rag.embedding.client;

import com.my.rag.embedding.dto.EmbeddingRequest;
import com.my.rag.embedding.dto.EmbeddingResponse;

public interface EmbeddingClient {

    EmbeddingResponse embed(EmbeddingRequest request);
}

package com.my.rag.chat.dto;

import com.my.rag.retrieval.dto.RetrievedChunk;

public record Evidence(String sourceId, RetrievedChunk chunk, String content) {}

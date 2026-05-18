package com.my.rag.api.chunk.dto;

import java.util.List;

public record ChunkListResponse(Long documentId, List<ChunkResponse> chunks) {}


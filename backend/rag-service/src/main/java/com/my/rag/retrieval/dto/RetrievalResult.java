package com.my.rag.retrieval.dto;

import java.util.List;

public record RetrievalResult(String question, boolean noEvidence, List<RetrievedChunk> chunks) {}


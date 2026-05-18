package com.my.rag.chat.dto;

import com.my.rag.retrieval.dto.RetrievedChunk;
import java.util.List;

public record GroundedChatContext(String question, List<RetrievedChunk> contexts) {}


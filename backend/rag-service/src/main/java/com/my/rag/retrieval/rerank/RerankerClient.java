package com.my.rag.retrieval.rerank;

import com.my.rag.retrieval.dto.HybridCandidate;
import java.util.List;

public interface RerankerClient {

    List<HybridCandidate> rerank(String question, List<HybridCandidate> candidates, int topK);
}

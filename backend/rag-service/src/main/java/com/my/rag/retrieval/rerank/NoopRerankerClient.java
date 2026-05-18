package com.my.rag.retrieval.rerank;

import com.my.rag.config.RagProperties;
import com.my.rag.retrieval.dto.HybridCandidate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NoopRerankerClient implements RerankerClient {

    private final RagProperties ragProperties;

    public NoopRerankerClient(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    @Override
    public List<HybridCandidate> rerank(String question, List<HybridCandidate> candidates, int topK) {
        if (!"noop".equalsIgnoreCase(ragProperties.getRetrieval().getRerankerProvider())) {
            throw new IllegalStateException("Unsupported reranker provider: "
                    + ragProperties.getRetrieval().getRerankerProvider());
        }
        return candidates.stream()
                .sorted(Comparator.comparingDouble((HybridCandidate candidate) ->
                        candidate.rrfScore() == null ? 0.0d : candidate.rrfScore()).reversed())
                .limit(Math.max(1, topK))
                .toList();
    }
}

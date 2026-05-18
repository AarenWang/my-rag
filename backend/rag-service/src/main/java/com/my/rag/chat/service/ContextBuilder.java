package com.my.rag.chat.service;

import com.my.rag.chat.dto.Evidence;
import com.my.rag.chat.dto.EvidencePack;
import com.my.rag.config.RagProperties;
import com.my.rag.retrieval.dto.RetrievedChunk;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ContextBuilder {

    private final RagProperties ragProperties;

    public ContextBuilder(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    public EvidencePack build(String question, List<RetrievedChunk> rankedChunks) {
        int contextTopK = Math.max(1, ragProperties.getRetrieval().getContextTopK());
        int maxContextChars = Math.max(500, ragProperties.getRetrieval().getMaxContextChars());
        Set<Long> seenChunkIds = new HashSet<>();
        Set<String> seenAdjacentKeys = new HashSet<>();
        List<Evidence> evidences = new java.util.ArrayList<>();

        int usedChars = 0;
        for (RetrievedChunk chunk : rankedChunks) {
            if (chunk == null || chunk.chunkId() == null || !StringUtils.hasText(chunk.content())) {
                continue;
            }
            if (!seenChunkIds.add(chunk.chunkId())) {
                continue;
            }
            if (!acceptAdjacentChunk(chunk, seenAdjacentKeys)) {
                continue;
            }
            if (evidences.size() >= contextTopK) {
                break;
            }

            int remainingChars = maxContextChars - usedChars;
            if (remainingChars <= 0) {
                break;
            }

            String content = truncate(chunk.content(), remainingChars);
            if (!StringUtils.hasText(content)) {
                break;
            }
            usedChars += content.length();
            evidences.add(new Evidence("source_" + (evidences.size() + 1), chunk, content));
        }

        return new EvidencePack(question, List.copyOf(evidences));
    }

    public String toPromptContext(EvidencePack evidencePack) {
        StringBuilder builder = new StringBuilder();
        for (Evidence evidence : evidencePack.evidences()) {
            RetrievedChunk chunk = evidence.chunk();
            builder.append('[').append(evidence.sourceId()).append("]\n");
            builder.append("书名：").append(nullToDash(chunk.documentTitle())).append('\n');
            builder.append("章节：").append(nullToDash(chunk.chapterTitle())).append('\n');
            builder.append("chunkId：").append(chunk.chunkId()).append('\n');
            builder.append("召回方式：").append(chunk.retrievalSources().isEmpty()
                    ? "-"
                    : String.join(", ", chunk.retrievalSources())).append('\n');
            builder.append("vectorRank：").append(nullToDash(chunk.vectorRank())).append('\n');
            builder.append("keywordRank：").append(nullToDash(chunk.keywordRank())).append('\n');
            builder.append("rrfScore：").append(nullToDash(chunk.rrfScore())).append('\n');
            builder.append("finalScore：").append(nullToDash(chunk.finalScore())).append('\n');
            builder.append("内容：").append(evidence.content()).append("\n\n");
        }
        return builder.toString();
    }

    private boolean acceptAdjacentChunk(RetrievedChunk chunk, Set<String> seenAdjacentKeys) {
        if (chunk.documentId() == null || chunk.chunkIndex() == null) {
            return true;
        }
        String chapter = chunk.chapterTitle() == null ? "" : chunk.chapterTitle();
        int bucket = chunk.chunkIndex() / 2;
        String key = chunk.documentId() + "|" + chapter + "|" + bucket;
        return seenAdjacentKeys.add(key);
    }

    private String truncate(String content, int maxChars) {
        if (content.length() <= maxChars) {
            return content;
        }
        if (maxChars <= 20) {
            return "";
        }
        return content.substring(0, maxChars - 3) + "...";
    }

    private String nullToDash(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}

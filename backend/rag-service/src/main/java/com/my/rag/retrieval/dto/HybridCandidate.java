package com.my.rag.retrieval.dto;

import java.util.ArrayList;
import java.util.List;

public class HybridCandidate {

    private final RetrievedChunk chunk;
    private Double vectorScore;
    private Double keywordScore;
    private Double rrfScore;
    private Double rerankScore;
    private Integer vectorRank;
    private Integer keywordRank;

    public HybridCandidate(RetrievedChunk chunk) {
        this.chunk = chunk;
    }

    public void addVectorHit(int rank, Double score) {
        this.vectorRank = rank;
        this.vectorScore = score;
    }

    public void addKeywordHit(int rank, Double score) {
        this.keywordRank = rank;
        this.keywordScore = score;
    }

    public void calculateRrfScore(int rrfK) {
        double score = 0.0d;
        if (vectorRank != null) {
            score += 1.0d / (rrfK + vectorRank);
        }
        if (keywordRank != null) {
            score += 1.0d / (rrfK + keywordRank);
        }
        this.rrfScore = score;
    }

    public RetrievedChunk chunk() {
        return chunk;
    }

    public Double rrfScore() {
        return rrfScore;
    }

    public Double rerankScore() {
        return rerankScore;
    }

    public void setRerankScore(Double rerankScore) {
        this.rerankScore = rerankScore;
    }

    public RetrievedChunk toRetrievedChunk() {
        double finalScore = rerankScore != null
                ? rerankScore
                : rrfScore != null ? rrfScore : nullToZero(chunk.score());
        return new RetrievedChunk(
                chunk.documentId(),
                chunk.documentTitle(),
                chunk.chapterTitle(),
                chunk.chunkId(),
                chunk.chunkIndex(),
                chunk.startParagraph(),
                chunk.endParagraph(),
                chunk.content(),
                finalScore,
                vectorScore,
                keywordScore,
                rrfScore,
                rerankScore,
                finalScore,
                vectorRank,
                keywordRank,
                retrievalSources());
    }

    private List<String> retrievalSources() {
        List<String> sources = new ArrayList<>();
        if (vectorRank != null) {
            sources.add("vector");
        }
        if (keywordRank != null) {
            sources.add("keyword");
        }
        return List.copyOf(sources);
    }

    private double nullToZero(Double value) {
        return value == null ? 0.0d : value;
    }
}

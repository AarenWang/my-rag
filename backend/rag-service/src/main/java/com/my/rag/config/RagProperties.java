package com.my.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private final Chunk chunk = new Chunk();
    private final Retrieval retrieval = new Retrieval();
    private final Model model = new Model();

    public Chunk getChunk() {
        return chunk;
    }

    public Retrieval getRetrieval() {
        return retrieval;
    }

    public Model getModel() {
        return model;
    }

    public static class Chunk {
        private int minChars = 300;
        private int maxChars = 1000;
        private int overlapChars = 150;

        public int getMinChars() {
            return minChars;
        }

        public void setMinChars(int minChars) {
            this.minChars = minChars;
        }

        public int getMaxChars() {
            return maxChars;
        }

        public void setMaxChars(int maxChars) {
            this.maxChars = maxChars;
        }

        public int getOverlapChars() {
            return overlapChars;
        }

        public void setOverlapChars(int overlapChars) {
            this.overlapChars = overlapChars;
        }
    }

    public static class Retrieval {
        private int defaultTopK = 20;
        private int contextTopK = 8;
        private double scoreThreshold = 0.35;

        public int getDefaultTopK() {
            return defaultTopK;
        }

        public void setDefaultTopK(int defaultTopK) {
            this.defaultTopK = defaultTopK;
        }

        public int getContextTopK() {
            return contextTopK;
        }

        public void setContextTopK(int contextTopK) {
            this.contextTopK = contextTopK;
        }

        public double getScoreThreshold() {
            return scoreThreshold;
        }

        public void setScoreThreshold(double scoreThreshold) {
            this.scoreThreshold = scoreThreshold;
        }
    }

    public static class Model {
        private String embeddingModel = "bge-m3";
        private int embeddingDimension = 1024;
        private String chatModel = "deepseek-chat";

        public String getEmbeddingModel() {
            return embeddingModel;
        }

        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        public int getEmbeddingDimension() {
            return embeddingDimension;
        }

        public void setEmbeddingDimension(int embeddingDimension) {
            this.embeddingDimension = embeddingDimension;
        }

        public String getChatModel() {
            return chatModel;
        }

        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }
    }
}


package com.my.rag.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private final Chunk chunk = new Chunk();
    private final Index index = new Index();
    private final Retrieval retrieval = new Retrieval();
    private final Model model = new Model();
    private final Storage storage = new Storage();

    public Chunk getChunk() {
        return chunk;
    }

    public Index getIndex() {
        return index;
    }

    public Retrieval getRetrieval() {
        return retrieval;
    }

    public Model getModel() {
        return model;
    }

    public Storage getStorage() {
        return storage;
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

    public static class Index {
        private int workerThreads = 1;
        private int queueCapacity = 20;
        private int tikaMaxExtractChars = 5_000_000;

        public int getWorkerThreads() {
            return workerThreads;
        }

        public void setWorkerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public int getTikaMaxExtractChars() {
            return tikaMaxExtractChars;
        }

        public void setTikaMaxExtractChars(int tikaMaxExtractChars) {
            this.tikaMaxExtractChars = tikaMaxExtractChars;
        }
    }

    public static class Retrieval {
        private String mode = "vector";
        private int defaultTopK = 20;
        private int vectorTopK = 20;
        private int keywordTopK = 20;
        private int rrfTopK = 30;
        private int rerankTopK = 8;
        private int contextTopK = 8;
        private double scoreThreshold = 0.35;
        private int rrfK = 60;
        private String textSearchConfig = "jiebacfg";
        private boolean keywordIndexEnabled = false;
        private String keywordQueryMode = "websearch";
        private int keywordMaxQueries = 3;
        private String rerankerProvider = "noop";
        private int maxContextChars = 8000;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public int getDefaultTopK() {
            return defaultTopK;
        }

        public void setDefaultTopK(int defaultTopK) {
            this.defaultTopK = defaultTopK;
        }

        public int getVectorTopK() {
            return vectorTopK;
        }

        public void setVectorTopK(int vectorTopK) {
            this.vectorTopK = vectorTopK;
        }

        public int getKeywordTopK() {
            return keywordTopK;
        }

        public void setKeywordTopK(int keywordTopK) {
            this.keywordTopK = keywordTopK;
        }

        public int getRrfTopK() {
            return rrfTopK;
        }

        public void setRrfTopK(int rrfTopK) {
            this.rrfTopK = rrfTopK;
        }

        public int getRerankTopK() {
            return rerankTopK;
        }

        public void setRerankTopK(int rerankTopK) {
            this.rerankTopK = rerankTopK;
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

        public int getRrfK() {
            return rrfK;
        }

        public void setRrfK(int rrfK) {
            this.rrfK = rrfK;
        }

        public String getTextSearchConfig() {
            return textSearchConfig;
        }

        public void setTextSearchConfig(String textSearchConfig) {
            this.textSearchConfig = textSearchConfig;
        }

        public boolean isKeywordIndexEnabled() {
            return keywordIndexEnabled;
        }

        public void setKeywordIndexEnabled(boolean keywordIndexEnabled) {
            this.keywordIndexEnabled = keywordIndexEnabled;
        }

        public String getKeywordQueryMode() {
            return keywordQueryMode;
        }

        public void setKeywordQueryMode(String keywordQueryMode) {
            this.keywordQueryMode = keywordQueryMode;
        }

        public int getKeywordMaxQueries() {
            return keywordMaxQueries;
        }

        public void setKeywordMaxQueries(int keywordMaxQueries) {
            this.keywordMaxQueries = keywordMaxQueries;
        }

        public String getRerankerProvider() {
            return rerankerProvider;
        }

        public void setRerankerProvider(String rerankerProvider) {
            this.rerankerProvider = rerankerProvider;
        }

        public int getMaxContextChars() {
            return maxContextChars;
        }

        public void setMaxContextChars(int maxContextChars) {
            this.maxContextChars = maxContextChars;
        }
    }

    public static class Model {
        private String embeddingProvider = "mock";
        private String embeddingModel = "bge-m3";
        private int embeddingDimension = 1024;
        private int embeddingBatchSize = 32;
        private String embeddingBaseUrl = "https://api.openai.com/v1";
        private String embeddingApiKey;
        private int embeddingRequestTimeoutSeconds = 60;
        private BigDecimal embeddingPricePer1kTokens = new BigDecimal("0.0005");
        private String chatProvider = "mock";
        private String chatModel = "deepseek-chat";
        private String chatBaseUrl = "https://api.openai.com/v1";
        private String chatApiKey;
        private int chatRequestTimeoutSeconds = 60;
        private Double chatTemperature = 0.2;

        public String getEmbeddingProvider() {
            return embeddingProvider;
        }

        public void setEmbeddingProvider(String embeddingProvider) {
            this.embeddingProvider = embeddingProvider;
        }

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

        public int getEmbeddingBatchSize() {
            return embeddingBatchSize;
        }

        public void setEmbeddingBatchSize(int embeddingBatchSize) {
            this.embeddingBatchSize = embeddingBatchSize;
        }

        public String getEmbeddingBaseUrl() {
            return embeddingBaseUrl;
        }

        public void setEmbeddingBaseUrl(String embeddingBaseUrl) {
            this.embeddingBaseUrl = embeddingBaseUrl;
        }

        public String getEmbeddingApiKey() {
            return embeddingApiKey;
        }

        public void setEmbeddingApiKey(String embeddingApiKey) {
            this.embeddingApiKey = embeddingApiKey;
        }

        public int getEmbeddingRequestTimeoutSeconds() {
            return embeddingRequestTimeoutSeconds;
        }

        public void setEmbeddingRequestTimeoutSeconds(int embeddingRequestTimeoutSeconds) {
            this.embeddingRequestTimeoutSeconds = embeddingRequestTimeoutSeconds;
        }

        public BigDecimal getEmbeddingPricePer1kTokens() {
            return embeddingPricePer1kTokens;
        }

        public void setEmbeddingPricePer1kTokens(BigDecimal embeddingPricePer1kTokens) {
            this.embeddingPricePer1kTokens = embeddingPricePer1kTokens;
        }

        public String getChatModel() {
            return chatModel;
        }

        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }

        public String getChatProvider() {
            return chatProvider;
        }

        public void setChatProvider(String chatProvider) {
            this.chatProvider = chatProvider;
        }

        public String getChatBaseUrl() {
            return chatBaseUrl;
        }

        public void setChatBaseUrl(String chatBaseUrl) {
            this.chatBaseUrl = chatBaseUrl;
        }

        public String getChatApiKey() {
            return chatApiKey;
        }

        public void setChatApiKey(String chatApiKey) {
            this.chatApiKey = chatApiKey;
        }

        public int getChatRequestTimeoutSeconds() {
            return chatRequestTimeoutSeconds;
        }

        public void setChatRequestTimeoutSeconds(int chatRequestTimeoutSeconds) {
            this.chatRequestTimeoutSeconds = chatRequestTimeoutSeconds;
        }

        public Double getChatTemperature() {
            return chatTemperature;
        }

        public void setChatTemperature(Double chatTemperature) {
            this.chatTemperature = chatTemperature;
        }
    }

    public static class Storage {
        private String uploadDir = "uploads/documents";

        public String getUploadDir() {
            return uploadDir;
        }

        public void setUploadDir(String uploadDir) {
            this.uploadDir = uploadDir;
        }
    }
}

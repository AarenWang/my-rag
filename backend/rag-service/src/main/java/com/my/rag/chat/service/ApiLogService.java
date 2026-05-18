package com.my.rag.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.rag.chat.dto.LlmChatRequest;
import com.my.rag.chat.dto.LlmChatResponse;
import com.my.rag.chat.entity.RagLlmApiLog;
import com.my.rag.chat.repository.RagLlmApiLogMapper;
import com.my.rag.embedding.dto.EmbeddingInput;
import com.my.rag.embedding.dto.EmbeddingRequest;
import com.my.rag.embedding.dto.EmbeddingResponse;
import com.my.rag.embedding.entity.RagEmbeddingApiLog;
import com.my.rag.embedding.repository.RagEmbeddingApiLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiLogService {

    private static final Logger log = LoggerFactory.getLogger(ApiLogService.class);

    private final ObjectMapper objectMapper;
    private final RagLlmApiLogMapper llmApiLogMapper;
    private final RagEmbeddingApiLogMapper embeddingApiLogMapper;

    public ApiLogService(
            ObjectMapper objectMapper,
            RagLlmApiLogMapper llmApiLogMapper,
            RagEmbeddingApiLogMapper embeddingApiLogMapper) {
        this.objectMapper = objectMapper;
        this.llmApiLogMapper = llmApiLogMapper;
        this.embeddingApiLogMapper = embeddingApiLogMapper;
    }

    @Transactional
    public void logLlmApiCall(
            Long chatLogId,
            String model,
            LlmChatRequest request,
            LlmChatResponse response,
            Exception error,
            long latencyMs) {
        try {
            RagLlmApiLog apiLog = new RagLlmApiLog();
            apiLog.setChatLogId(chatLogId);
            apiLog.setModel(model);
            apiLog.setRequest(objectMapper.writeValueAsString(request));
            apiLog.setLatencyMs(latencyMs);
            apiLog.setIsSuccess(error == null);

            if (response != null) {
                apiLog.setResponse(objectMapper.writeValueAsString(response));
                apiLog.setPromptTokens(response.promptTokens());
                apiLog.setCompletionTokens(response.completionTokens());
                apiLog.setTotalTokens(
                        (response.promptTokens() == null ? 0 : response.promptTokens()) +
                                (response.completionTokens() == null ? 0 : response.completionTokens())
                );
            }

            if (error != null) {
                apiLog.setErrorMessage(error.getMessage());
            }

            llmApiLogMapper.insert(apiLog);
            log.debug("LLM API call logged, model: {}, latency: {}ms, success: {}",
                    model, latencyMs, error == null);
        } catch (Exception e) {
            log.error("Failed to log LLM API call", e);
        }
    }

    @Transactional
    public void logEmbeddingApiCall(
            Long documentId,
            Long chunkId,
            String model,
            EmbeddingInput input,
            EmbeddingResponse response,
            Exception error,
            long latencyMs) {
        try {
            RagEmbeddingApiLog apiLog = new RagEmbeddingApiLog();
            apiLog.setDocumentId(documentId);
            apiLog.setChunkId(chunkId);
            apiLog.setModel(model);
            apiLog.setInputText(input.text());
            apiLog.setInputLength(input.text() == null ? 0 : input.text().length());
            apiLog.setLatencyMs(latencyMs);
            apiLog.setIsSuccess(error == null);

            if (response != null) {
                apiLog.setResponse(objectMapper.writeValueAsString(response));
            }

            if (error != null) {
                apiLog.setErrorMessage(error.getMessage());
            }

            embeddingApiLogMapper.insert(apiLog);
            log.debug("Embedding API call logged, model: {}, chunkId: {}, latency: {}ms, success: {}",
                    model, chunkId, latencyMs, error == null);
        } catch (Exception e) {
            log.error("Failed to log Embedding API call", e);
        }
    }
}

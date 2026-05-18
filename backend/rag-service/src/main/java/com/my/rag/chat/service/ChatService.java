package com.my.rag.chat.service;

import com.my.rag.api.chat.dto.ChatRequest;
import com.my.rag.api.chat.dto.ChatResponse;
import com.my.rag.chat.client.LlmClient;
import com.my.rag.chat.dto.LlmChatRequest;
import com.my.rag.chat.dto.LlmChatResponse;
import com.my.rag.chat.dto.LlmMessage;
import com.my.rag.chat.entity.RagChatLog;
import com.my.rag.chat.repository.RagChatLogMapper;
import com.my.rag.chat.service.ApiLogService;
import com.my.rag.config.RagProperties;
import com.my.rag.retrieval.dto.RetrievalQuery;
import com.my.rag.retrieval.dto.RetrievalResult;
import com.my.rag.retrieval.dto.RetrievedChunk;
import com.my.rag.retrieval.service.RetrievalService;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {

    private static final String NO_EVIDENCE_ANSWER = "当前资料中没有找到明确依据。";

    private final RagProperties ragProperties;
    private final RetrievalService retrievalService;
    private final LlmClient llmClient;
    private final RagChatLogMapper chatLogMapper;
    private final ApiLogService apiLogService;

    public ChatService(
            RagProperties ragProperties,
            RetrievalService retrievalService,
            LlmClient llmClient,
            RagChatLogMapper chatLogMapper,
            ApiLogService apiLogService) {
        this.ragProperties = ragProperties;
        this.retrievalService = retrievalService;
        this.llmClient = llmClient;
        this.chatLogMapper = chatLogMapper;
        this.apiLogService = apiLogService;
    }

    public ChatResponse chat(ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.question())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question must not be blank");
        }

        long startedAt = System.nanoTime();
        ChatResponse response = null;
        List<RetrievedChunk> retrievedChunks = List.of();

        RagChatLog chatLog = new RagChatLog();
        chatLog.setQuestion(request.question());
        
        Long chatLogId = null;
        try {
            int retrievalTopK = resolveTopK(request.topK());
            double scoreThreshold = resolveScoreThreshold(request.scoreThreshold());
            RetrievalResult retrievalResult = retrievalService.retrieve(new RetrievalQuery(
                    request.question(),
                    null,
                    request.documentIds(),
                    retrievalTopK,
                    scoreThreshold));
            retrievedChunks = retrievalResult.chunks();

            if (retrievalResult.noEvidence()) {
                response = new ChatResponse(NO_EVIDENCE_ANSWER, true, List.of());
                chatLog.setAnswer(response.answer());
                chatLog.setDocumentIds(joinIds(request.documentIds()));
                chatLog.setRetrievedChunkIds("");
                chatLog.setTopK(resolveTopK(request.topK()));
                chatLog.setLatencyMs(elapsedMillis(startedAt));
                chatLogMapper.insert(chatLog);
                return response;
            }

            List<RetrievedChunk> contexts = selectContexts(retrievedChunks);
            String model = ragProperties.getModel().getChatModel();
            LlmChatRequest llmRequest = new LlmChatRequest(
                    model,
                    buildMessages(request.question(), contexts),
                    ragProperties.getModel().getChatTemperature());

            long llmStartedAt = System.nanoTime();
            LlmChatResponse llmResponse = null;
            Exception llmError = null;
            try {
                llmResponse = llmClient.chat(llmRequest);
            } catch (Exception e) {
                llmError = e;
                throw e;
            } finally {
                long llmLatency = elapsedMillis(llmStartedAt);
                apiLogService.logLlmApiCall(
                        chatLogId,
                        model,
                        llmRequest,
                        llmResponse,
                        llmError,
                        llmLatency);
            }

            response = new ChatResponse(withSourceReminder(llmResponse.content(), contexts), false, toSources(contexts));
            
            chatLog.setAnswer(response.answer());
            chatLog.setDocumentIds(joinIds(request.documentIds()));
            chatLog.setRetrievedChunkIds(retrievedChunks.stream()
                    .map(RetrievedChunk::chunkId)
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
            chatLog.setTopK(resolveTopK(request.topK()));
            chatLog.setMinScore(retrievedChunks.stream()
                    .map(RetrievedChunk::score)
                    .filter(Objects::nonNull)
                    .min(Double::compareTo)
                    .orElse(null));
            chatLog.setLatencyMs(elapsedMillis(startedAt));
            chatLogMapper.insert(chatLog);
            chatLogId = chatLog.getId();
            
            return response;
        } finally {
            if (chatLogId == null && chatLog.getAnswer() != null) {
                chatLogMapper.insert(chatLog);
            }
        }
    }

    private List<LlmMessage> buildMessages(String question, List<RetrievedChunk> contexts) {
        return List.of(
                LlmMessage.system("""
                        你是一个中文电子书知识库问答助手。请只根据用户提供的资料片段回答问题。
                        如果片段中没有足够依据，必须回答“当前资料中没有找到明确依据。”。
                        不要编造资料中没有出现的信息。回答后列出引用来源，格式包含 source 编号、书名、章节和 chunkId。
                        如果多个片段观点不一致，请说明差异。
                        """),
                LlmMessage.user("用户问题：\n" + question + "\n\n检索片段：\n" + formatContexts(contexts)));
    }

    private String formatContexts(List<RetrievedChunk> contexts) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < contexts.size(); i++) {
            RetrievedChunk chunk = contexts.get(i);
            builder.append("[source_").append(i + 1).append("]\n");
            builder.append("书名：").append(nullToDash(chunk.documentTitle())).append('\n');
            builder.append("章节：").append(nullToDash(chunk.chapterTitle())).append('\n');
            builder.append("chunkId：").append(chunk.chunkId()).append('\n');
            builder.append("score：").append(chunk.score()).append('\n');
            builder.append("内容：").append(chunk.content()).append("\n\n");
        }
        return builder.toString();
    }

    private String withSourceReminder(String answer, List<RetrievedChunk> contexts) {
        if (answer == null) {
            return NO_EVIDENCE_ANSWER;
        }
        if (answer.contains("source_") || answer.contains("引用来源")) {
            return answer;
        }

        String refs = contexts.stream()
                .map(chunk -> "source_" + (contexts.indexOf(chunk) + 1)
                        + ": " + nullToDash(chunk.documentTitle())
                        + " / " + nullToDash(chunk.chapterTitle())
                        + " / chunkId " + chunk.chunkId())
                .collect(Collectors.joining("\n"));
        return answer + "\n\n引用来源：\n" + refs;
    }

    private List<RetrievedChunk> selectContexts(List<RetrievedChunk> chunks) {
        int contextTopK = Math.max(1, ragProperties.getRetrieval().getContextTopK());
        return chunks.stream().limit(contextTopK).toList();
    }

    private List<ChatResponse.Source> toSources(List<RetrievedChunk> chunks) {
        return chunks.stream()
                .map(chunk -> new ChatResponse.Source(
                        chunk.documentId(),
                        chunk.documentTitle(),
                        chunk.chapterTitle(),
                        chunk.chunkId(),
                        chunk.chunkIndex(),
                        chunk.score()))
                .toList();
    }

    private int resolveTopK(Integer requestedTopK) {
        return requestedTopK != null && requestedTopK > 0
                ? requestedTopK
                : Math.max(1, ragProperties.getRetrieval().getDefaultTopK());
    }

    private double resolveScoreThreshold(Double requestedScoreThreshold) {
        return requestedScoreThreshold != null
                ? requestedScoreThreshold
                : ragProperties.getRetrieval().getScoreThreshold();
    }

    private String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private String nullToDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}

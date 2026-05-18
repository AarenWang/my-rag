package com.my.rag.chat.service;

import com.my.rag.api.chat.dto.ChatLogDetailResponse;
import com.my.rag.api.chat.dto.ChatLogSummaryResponse;
import com.my.rag.api.chat.dto.ChatRequest;
import com.my.rag.api.chat.dto.ChatResponse;
import com.my.rag.chat.client.LlmClient;
import com.my.rag.chat.dto.LlmChatRequest;
import com.my.rag.chat.dto.LlmChatResponse;
import com.my.rag.chat.dto.LlmMessage;
import com.my.rag.chat.dto.Evidence;
import com.my.rag.chat.dto.EvidencePack;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String NO_EVIDENCE_ANSWER = "当前资料中没有找到明确依据。";

    private final RagProperties ragProperties;
    private final RetrievalService retrievalService;
    private final LlmClient llmClient;
    private final RagChatLogMapper chatLogMapper;
    private final ApiLogService apiLogService;
    private final ContextBuilder contextBuilder;

    public ChatService(
            RagProperties ragProperties,
            RetrievalService retrievalService,
            LlmClient llmClient,
            RagChatLogMapper chatLogMapper,
            ApiLogService apiLogService,
            ContextBuilder contextBuilder) {
        this.ragProperties = ragProperties;
        this.retrievalService = retrievalService;
        this.llmClient = llmClient;
        this.chatLogMapper = chatLogMapper;
        this.apiLogService = apiLogService;
        this.contextBuilder = contextBuilder;
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
        chatLogMapper.insert(chatLog);
        Long chatLogId = chatLog.getId();
        
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
                chatLogMapper.updateById(chatLog);
                return response;
            }

            EvidencePack evidencePack = contextBuilder.build(request.question(), retrievedChunks);
            if (evidencePack.evidences().isEmpty()) {
                response = new ChatResponse(NO_EVIDENCE_ANSWER, true, List.of());
                chatLog.setAnswer(response.answer());
                chatLog.setDocumentIds(joinIds(request.documentIds()));
                chatLog.setRetrievedChunkIds("");
                chatLog.setTopK(resolveTopK(request.topK()));
                chatLog.setLatencyMs(elapsedMillis(startedAt));
                chatLogMapper.updateById(chatLog);
                return response;
            }

            String model = ragProperties.getModel().getChatModel();
            LlmChatRequest llmRequest = new LlmChatRequest(
                    model,
                    buildMessages(evidencePack),
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

            response = new ChatResponse(withSourceReminder(llmResponse.content(), evidencePack), false, toSources(evidencePack));
            
            chatLog.setAnswer(response.answer());
            chatLog.setDocumentIds(joinIds(request.documentIds()));
            chatLog.setRetrievedChunkIds(evidencePack.evidences().stream()
                    .map(Evidence::chunk)
                    .map(RetrievedChunk::chunkId)
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
            chatLog.setTopK(resolveTopK(request.topK()));
            chatLog.setMinScore(evidencePack.evidences().stream()
                    .map(Evidence::chunk)
                    .map(RetrievedChunk::finalScore)
                    .filter(Objects::nonNull)
                    .min(Double::compareTo)
                    .orElse(null));
            chatLog.setLatencyMs(elapsedMillis(startedAt));
            chatLogMapper.updateById(chatLog);
            
            return response;
        } catch (Exception e) {
            log.error("Chat failed, chatLogId: {}", chatLogId, e);
            throw e;
        }
    }

    private List<LlmMessage> buildMessages(EvidencePack evidencePack) {
        return List.of(
                LlmMessage.system("""
                        你是一个中文电子书知识库问答助手。请只根据用户提供的资料片段回答问题。
                        如果片段中没有足够依据，必须回答“当前资料中没有找到明确依据。”。
                        不要编造资料中没有出现的信息。回答后列出引用来源，格式为 [source_1]、[source_2]。
                        如果多个片段观点不一致，请说明差异。
                        不要输出与问题无关的泛泛解释。
                        """),
                LlmMessage.user("用户问题：\n" + evidencePack.question()
                        + "\n\n资料：\n" + contextBuilder.toPromptContext(evidencePack)
                        + "\n请给出回答："));
    }

    private String withSourceReminder(String answer, EvidencePack evidencePack) {
        if (answer == null) {
            return NO_EVIDENCE_ANSWER;
        }
        if (answer.contains("source_") || answer.contains("引用来源")) {
            return answer;
        }

        String refs = evidencePack.evidences().stream()
                .map(evidence -> evidence.sourceId()
                        + ": " + nullToDash(evidence.chunk().documentTitle())
                        + " / " + nullToDash(evidence.chunk().chapterTitle())
                        + " / chunkId " + evidence.chunk().chunkId())
                .collect(Collectors.joining("\n"));
        return answer + "\n\n引用来源：\n" + refs;
    }

    private List<ChatResponse.Source> toSources(EvidencePack evidencePack) {
        return evidencePack.evidences().stream()
                .map(Evidence::chunk)
                .map(chunk -> new ChatResponse.Source(
                        chunk.documentId(),
                        chunk.documentTitle(),
                        chunk.chapterTitle(),
                        chunk.chunkId(),
                        chunk.chunkIndex(),
                        chunk.finalScore()))
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

    public List<ChatLogSummaryResponse> listChatLogs() {
        log.info("Listing chat logs");
        List<RagChatLog> logs = chatLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RagChatLog>()
                        .orderByDesc(RagChatLog::getCreatedAt)
        );
        return logs.stream()
                .map(log -> new ChatLogSummaryResponse(
                        log.getId(),
                        log.getQuestion(),
                        log.getAnswer() == null ? "" : truncateAnswer(log.getAnswer(), 100),
                        log.getCreatedAt()))
                .toList();
    }

    public ChatLogDetailResponse getChatLogDetail(Long id) {
        log.info("Getting chat log detail, id: {}", id);
        RagChatLog log = chatLogMapper.selectById(id);
        if (log == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat log not found: " + id);
        }
        List<String> chunkIds = parseIds(log.getRetrievedChunkIds());
        return new ChatLogDetailResponse(
                log.getId(),
                log.getQuestion(),
                log.getAnswer(),
                log.getDocumentIds(),
                chunkIds,
                log.getTopK(),
                log.getMinScore(),
                log.getLatencyMs(),
                log.getCreatedAt());
    }

    private String truncateAnswer(String answer, int maxLength) {
        if (answer == null || answer.length() <= maxLength) {
            return answer;
        }
        return answer.substring(0, maxLength) + "...";
    }

    private List<String> parseIds(String idsStr) {
        if (idsStr == null || idsStr.isBlank()) {
            return List.of();
        }
        return List.of(idsStr.split(","));
    }
}

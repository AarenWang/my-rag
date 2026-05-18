package com.my.rag.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.my.rag.api.document.dto.DocumentIndexProgressResponse;
import com.my.rag.api.document.dto.DocumentIndexResponse;
import com.my.rag.chunk.entity.RagDocumentChunk;
import com.my.rag.chunk.repository.RagDocumentChunkMapper;
import com.my.rag.document.entity.RagDocument;
import com.my.rag.document.enums.DocumentStatus;
import com.my.rag.document.repository.RagDocumentMapper;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentIndexTaskService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexTaskService.class);
    private static final Set<DocumentStatus> INDEX_SUBMITTABLE_STATUSES = Set.of(
            DocumentStatus.UPLOADED,
            DocumentStatus.PARSED,
            DocumentStatus.CHUNKED,
            DocumentStatus.READY,
            DocumentStatus.FAILED);
    private static final Set<DocumentStatus> EMBEDDING_SUBMITTABLE_STATUSES = Set.of(
            DocumentStatus.CHUNKED,
            DocumentStatus.READY,
            DocumentStatus.FAILED);
    private static final Set<DocumentStatus> RUNNING_STATUSES = Set.of(
            DocumentStatus.PARSING,
            DocumentStatus.CHUNKING,
            DocumentStatus.EMBEDDING);

    private final RagDocumentMapper documentMapper;
    private final RagDocumentChunkMapper chunkMapper;
    private final DocumentLifecycleService lifecycleService;
    private final DocumentIndexService indexService;
    private final TaskExecutor indexTaskExecutor;
    private final ConcurrentHashMap<Long, ProgressSnapshot> progressByDocumentId = new ConcurrentHashMap<>();

    public DocumentIndexTaskService(
            RagDocumentMapper documentMapper,
            RagDocumentChunkMapper chunkMapper,
            DocumentLifecycleService lifecycleService,
            DocumentIndexService indexService,
            @Qualifier("indexTaskExecutor") TaskExecutor indexTaskExecutor) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.lifecycleService = lifecycleService;
        this.indexService = indexService;
        this.indexTaskExecutor = indexTaskExecutor;
    }

    public DocumentIndexResponse submitIndex(Long documentId) {
        RagDocument document = findDocumentOrThrow(documentId);
        DocumentStatus status = document.getStatus();

        if (RUNNING_STATUSES.contains(status) || isRunning(documentId)) {
            return new DocumentIndexResponse(documentId, "RUNNING", "Document index task is already running");
        }
        if (!INDEX_SUBMITTABLE_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot index document with status: " + status);
        }

        ProgressSnapshot queued = ProgressSnapshot.queued(documentId, document.getStatus().value(), countChunks(documentId));
        ProgressSnapshot existing = progressByDocumentId.putIfAbsent(documentId, queued);
        if (existing != null && existing.isRunning()) {
            return new DocumentIndexResponse(documentId, "RUNNING", "Document index task is already running");
        }
        if (existing != null) {
            progressByDocumentId.put(documentId, queued);
        }

        indexTaskExecutor.execute(() -> runIndex(documentId));
        return new DocumentIndexResponse(documentId, "QUEUED", "Document index task accepted");
    }

    public DocumentIndexResponse submitEmbedding(Long documentId) {
        RagDocument document = findDocumentOrThrow(documentId);
        DocumentStatus status = document.getStatus();

        if (RUNNING_STATUSES.contains(status) || isRunning(documentId)) {
            return new DocumentIndexResponse(documentId, "RUNNING", "Document task is already running");
        }
        if (!EMBEDDING_SUBMITTABLE_STATUSES.contains(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot generate embeddings with status: " + status);
        }
        if (countChunks(documentId) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot generate embeddings before chunks are created");
        }

        ProgressSnapshot queued = ProgressSnapshot.queued(documentId, document.getStatus().value(), countChunks(documentId));
        ProgressSnapshot existing = progressByDocumentId.putIfAbsent(documentId, queued);
        if (existing != null && existing.isRunning()) {
            return new DocumentIndexResponse(documentId, "RUNNING", "Document task is already running");
        }
        if (existing != null) {
            progressByDocumentId.put(documentId, queued);
        }

        indexTaskExecutor.execute(() -> runEmbedding(documentId));
        return new DocumentIndexResponse(documentId, "QUEUED", "Document embedding task accepted");
    }

    public DocumentIndexProgressResponse getProgress(Long documentId) {
        RagDocument document = findDocumentOrThrow(documentId);
        ProgressSnapshot snapshot = progressByDocumentId.get(documentId);
        if (snapshot == null) {
            int chunkCount = countChunks(documentId);
            snapshot = ProgressSnapshot.fromDocument(documentId, document.getStatus(), chunkCount, document.getErrorMessage());
        } else {
            snapshot = snapshot.withDocumentStatus(document.getStatus().value(), document.getErrorMessage(), countChunks(documentId));
        }
        return snapshot.toResponse();
    }

    private void runIndex(Long documentId) {
        updateProgress(documentId, "RUNNING", "QUEUED", 0, "Index task started", null, null);
        try {
            indexService.indexDocument(documentId, (stage, percent, message, chunkCount) ->
                    updateProgress(documentId, "RUNNING", stage, percent, message, chunkCount, null));
            updateProgress(documentId, "SUCCEEDED", "CHUNKED", 100, "Index task completed", countChunks(documentId), null);
        } catch (Throwable throwable) {
            log.error("Async index task failed, documentId: {}", documentId, throwable);
            String message = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
            markDocumentFailed(documentId, message);
            updateProgress(documentId, "FAILED", "FAILED", 100, "Index task failed", countChunks(documentId), message);
        }
    }

    private void runEmbedding(Long documentId) {
        updateProgress(documentId, "RUNNING", "QUEUED", 0, "Embedding task started", countChunks(documentId), null);
        try {
            indexService.embedDocument(documentId, (stage, percent, message, chunkCount) ->
                    updateProgress(documentId, "RUNNING", stage, percent, message, chunkCount, null));
            updateProgress(documentId, "SUCCEEDED", "READY", 100, "Embedding task completed", countChunks(documentId), null);
        } catch (Throwable throwable) {
            log.error("Async embedding task failed, documentId: {}", documentId, throwable);
            String message = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
            markDocumentFailed(documentId, message);
            updateProgress(documentId, "FAILED", "FAILED", 100, "Embedding task failed", countChunks(documentId), message);
        }
    }

    private void markDocumentFailed(Long documentId, String message) {
        RagDocument document = documentMapper.selectById(documentId);
        if (document != null && document.getStatus() != DocumentStatus.FAILED) {
            lifecycleService.fail(document, message);
            documentMapper.updateById(document);
        }
    }

    private boolean isRunning(Long documentId) {
        ProgressSnapshot snapshot = progressByDocumentId.get(documentId);
        return snapshot != null && snapshot.isRunning();
    }

    private RagDocument findDocumentOrThrow(Long documentId) {
        RagDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId);
        }
        return document;
    }

    private int countChunks(Long documentId) {
        Long count = chunkMapper.selectCount(
                new LambdaQueryWrapper<RagDocumentChunk>().eq(RagDocumentChunk::getDocumentId, documentId));
        return count == null ? 0 : Math.toIntExact(count);
    }

    private void updateProgress(
            Long documentId,
            String taskStatus,
            String stage,
            int progressPercent,
            String message,
            Integer chunkCount,
            String errorMessage) {
        progressByDocumentId.compute(documentId, (id, current) -> {
            int safeChunkCount = chunkCount != null ? chunkCount : current == null ? 0 : current.chunkCount();
            String documentStatus = current == null ? stage : current.documentStatus();
            return new ProgressSnapshot(
                    id,
                    documentStatus,
                    taskStatus,
                    stage,
                    Math.max(0, Math.min(100, progressPercent)),
                    safeChunkCount,
                    message,
                    errorMessage);
        });
    }

    private record ProgressSnapshot(
            Long documentId,
            String documentStatus,
            String taskStatus,
            String stage,
            int progressPercent,
            int chunkCount,
            String message,
            String errorMessage) {

        static ProgressSnapshot queued(Long documentId, String documentStatus, int chunkCount) {
            return new ProgressSnapshot(documentId, documentStatus, "QUEUED", "QUEUED", 0, chunkCount,
                    "Waiting for index worker", null);
        }

        static ProgressSnapshot fromDocument(
                Long documentId, DocumentStatus documentStatus, int chunkCount, String errorMessage) {
            int percent = switch (documentStatus) {
                case UPLOADED -> 0;
                case PARSING -> 20;
                case PARSED -> 40;
                case CHUNKING -> 70;
                case CHUNKED, READY -> 100;
                case EMBEDDING -> 90;
                case FAILED -> 100;
            };
            String taskStatus = documentStatus == DocumentStatus.FAILED
                    ? "FAILED"
                    : RUNNING_STATUSES.contains(documentStatus) ? "RUNNING" : "IDLE";
            return new ProgressSnapshot(documentId, documentStatus.value(), taskStatus, documentStatus.value(), percent,
                    chunkCount, "Progress restored from document status", errorMessage);
        }

        ProgressSnapshot withDocumentStatus(String status, String errorMessage, int chunkCount) {
            String visibleErrorMessage = "FAILED".equals(status)
                    ? errorMessage == null ? this.errorMessage : errorMessage
                    : null;
            return new ProgressSnapshot(documentId, status, taskStatus, stage, progressPercent, chunkCount, message,
                    visibleErrorMessage);
        }

        boolean isRunning() {
            return "QUEUED".equals(taskStatus) || "RUNNING".equals(taskStatus);
        }

        DocumentIndexProgressResponse toResponse() {
            return new DocumentIndexProgressResponse(
                    documentId, documentStatus, taskStatus, stage, progressPercent, chunkCount, message, errorMessage);
        }
    }
}

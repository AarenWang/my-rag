package com.my.rag.chunk.controller;

import com.my.rag.api.chunk.dto.ChunkListResponse;
import com.my.rag.api.chunk.dto.ChunkResponse;
import com.my.rag.chunk.entity.RagDocumentChunk;
import com.my.rag.chunk.service.ChunkService;
import com.my.rag.common.response.ApiResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/chunks")
public class ChunkController {

    private static final Logger log = LoggerFactory.getLogger(ChunkController.class);

    private final ChunkService chunkService;

    public ChunkController(ChunkService chunkService) {
        this.chunkService = chunkService;
    }

    @GetMapping
    public ApiResponse<ChunkListResponse> listChunks(@RequestParam Long documentId) {
        log.info("API request: GET /api/rag/chunks, documentId: {}", documentId);
        List<RagDocumentChunk> chunks = chunkService.getChunksByDocumentId(documentId);
        log.info("Retrieved {} chunks for documentId: {}", chunks.size(), documentId);
        List<ChunkResponse> chunkResponses = chunks.stream()
                .map(this::toChunkResponse)
                .toList();
        return ApiResponse.success(new ChunkListResponse(documentId, chunkResponses));
    }

    private ChunkResponse toChunkResponse(RagDocumentChunk chunk) {
        return new ChunkResponse(
                chunk.getId(),
                chunk.getDocumentId(),
                chunk.getChapterTitle(),
                chunk.getChunkIndex(),
                chunk.getStartParagraph(),
                chunk.getEndParagraph(),
                chunk.getContent(),
                chunk.getContentHash(),
                chunk.getTokenCount()
        );
    }
}

package com.my.rag.retrieval.controller;

import com.my.rag.api.retrieval.dto.RetrievalDebugRequest;
import com.my.rag.api.retrieval.dto.RetrievalDebugResponse;
import com.my.rag.common.response.ApiResponse;
import com.my.rag.retrieval.service.RetrievalDebugService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/retrieval")
public class RetrievalDebugController {

    private static final Logger log = LoggerFactory.getLogger(RetrievalDebugController.class);

    private final RetrievalDebugService retrievalDebugService;

    public RetrievalDebugController(RetrievalDebugService retrievalDebugService) {
        this.retrievalDebugService = retrievalDebugService;
    }

    @PostMapping("/debug")
    public ApiResponse<RetrievalDebugResponse> debug(@Valid @RequestBody RetrievalDebugRequest request) {
        log.info("API request: POST /api/rag/retrieval/debug, documentIds: {}, topK: {}",
                request.documentIds(), request.topK());
        return ApiResponse.success(retrievalDebugService.debug(request));
    }
}

package com.my.rag.document.controller;

import com.my.rag.api.document.dto.DocumentEmbeddingEstimateResponse;
import com.my.rag.api.document.dto.DocumentIndexProgressResponse;
import com.my.rag.api.document.dto.DocumentIndexResponse;
import com.my.rag.api.document.dto.DocumentStatusResponse;
import com.my.rag.api.document.dto.DocumentSummaryResponse;
import com.my.rag.api.document.dto.DocumentUploadResponse;
import com.my.rag.common.response.ApiResponse;
import com.my.rag.document.service.DocumentService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/rag/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ApiResponse<List<DocumentSummaryResponse>> listDocuments() {
        log.info("API request: GET /api/rag/documents");
        return ApiResponse.success(documentService.listDocuments());
    }

    @PostMapping("/upload")
    public ApiResponse<DocumentUploadResponse> uploadDocument(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "collectionId", required = false) Long collectionId) {
        log.info("API request: POST /api/rag/documents/upload, fileName: {}, collectionId: {}",
                file.getOriginalFilename(), collectionId);
        return ApiResponse.success(documentService.uploadDocument(file, collectionId));
    }

    @GetMapping("/{id}/status")
    public ApiResponse<DocumentStatusResponse> getDocumentStatus(@PathVariable Long id) {
        log.info("API request: GET /api/rag/documents/{}/status", id);
        return ApiResponse.success(documentService.getDocumentStatus(id));
    }

    @PostMapping("/{id}/index")
    public ApiResponse<DocumentIndexResponse> indexDocument(@PathVariable Long id) {
        log.info("API request: POST /api/rag/documents/{}/index", id);
        return ApiResponse.success(documentService.indexDocument(id));
    }

    @GetMapping("/{id}/embedding/estimate")
    public ApiResponse<DocumentEmbeddingEstimateResponse> getEmbeddingEstimate(@PathVariable Long id) {
        log.info("API request: GET /api/rag/documents/{}/embedding/estimate", id);
        return ApiResponse.success(documentService.getEmbeddingEstimate(id));
    }

    @PostMapping("/{id}/embedding")
    public ApiResponse<DocumentIndexResponse> embedDocument(@PathVariable Long id) {
        log.info("API request: POST /api/rag/documents/{}/embedding", id);
        return ApiResponse.success(documentService.embedDocument(id));
    }

    @GetMapping("/{id}/index/progress")
    public ApiResponse<DocumentIndexProgressResponse> getIndexProgress(@PathVariable Long id) {
        log.info("API request: GET /api/rag/documents/{}/index/progress", id);
        return ApiResponse.success(documentService.getIndexProgress(id));
    }
}

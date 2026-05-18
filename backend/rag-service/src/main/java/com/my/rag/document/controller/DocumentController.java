package com.my.rag.document.controller;

import com.my.rag.api.document.dto.DocumentStatusResponse;
import com.my.rag.api.document.dto.DocumentSummaryResponse;
import com.my.rag.api.document.dto.DocumentUploadResponse;
import com.my.rag.common.response.ApiResponse;
import com.my.rag.document.service.DocumentService;
import java.util.List;
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

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ApiResponse<List<DocumentSummaryResponse>> listDocuments() {
        return ApiResponse.success(documentService.listDocuments());
    }

    @PostMapping("/upload")
    public ApiResponse<DocumentUploadResponse> uploadDocument(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(documentService.uploadDocument(file));
    }

    @GetMapping("/{id}/status")
    public ApiResponse<DocumentStatusResponse> getDocumentStatus(@PathVariable Long id) {
        return ApiResponse.success(documentService.getDocumentStatus(id));
    }
}

package com.my.rag.document.controller;

import com.my.rag.api.document.dto.DocumentStatusResponse;
import com.my.rag.api.document.dto.DocumentSummaryResponse;
import com.my.rag.common.response.ApiResponse;
import com.my.rag.document.service.DocumentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/{id}/status")
    public ApiResponse<DocumentStatusResponse> getDocumentStatus(@PathVariable Long id) {
        return ApiResponse.success(documentService.getDocumentStatus(id));
    }
}

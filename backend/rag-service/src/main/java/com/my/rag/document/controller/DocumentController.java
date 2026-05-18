package com.my.rag.document.controller;

import com.my.rag.api.document.dto.DocumentStatusResponse;
import com.my.rag.api.document.dto.DocumentSummaryResponse;
import com.my.rag.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/documents")
public class DocumentController {

    @GetMapping
    public ApiResponse<List<DocumentSummaryResponse>> listDocuments() {
        return ApiResponse.success(List.of());
    }

    @GetMapping("/{id}/status")
    public ApiResponse<DocumentStatusResponse> getDocumentStatus(@PathVariable Long id) {
        return ApiResponse.success(new DocumentStatusResponse(id, "UPLOADED", null));
    }
}


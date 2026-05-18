package com.my.rag.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.my.rag.api.document.dto.DocumentStatusResponse;
import com.my.rag.api.document.dto.DocumentSummaryResponse;
import com.my.rag.document.entity.RagDocument;
import com.my.rag.document.repository.RagDocumentMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private final RagDocumentMapper documentMapper;

    public DocumentService(RagDocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    public List<DocumentSummaryResponse> listDocuments() {
        return documentMapper
                .selectList(
                        new LambdaQueryWrapper<RagDocument>()
                                .orderByDesc(RagDocument::getCreatedAt)
                                .orderByDesc(RagDocument::getId))
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public DocumentStatusResponse getDocumentStatus(Long documentId) {
        RagDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            return new DocumentStatusResponse(documentId, "NOT_FOUND", "Document not found");
        }
        return new DocumentStatusResponse(
                document.getId(), document.getStatus().value(), document.getErrorMessage());
    }

    private DocumentSummaryResponse toSummaryResponse(RagDocument document) {
        return new DocumentSummaryResponse(
                document.getId(),
                document.getTitle(),
                document.getFileName(),
                document.getFileType(),
                document.getStatus().value());
    }
}


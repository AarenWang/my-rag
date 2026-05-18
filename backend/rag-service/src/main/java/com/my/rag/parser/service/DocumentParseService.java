package com.my.rag.parser.service;

import com.my.rag.document.entity.RagDocument;
import com.my.rag.document.enums.DocumentStatus;
import com.my.rag.document.repository.RagDocumentMapper;
import com.my.rag.document.service.DocumentLifecycleService;
import com.my.rag.parser.dto.ParsedDocument;
import com.my.rag.parser.strategy.DocumentParser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DocumentParseService {

    private final List<DocumentParser> parsers;
    private final DocumentLifecycleService lifecycleService;
    private final RagDocumentMapper documentMapper;

    public DocumentParseService(
            List<DocumentParser> parsers,
            DocumentLifecycleService lifecycleService,
            RagDocumentMapper documentMapper) {
        this.parsers = parsers;
        this.lifecycleService = lifecycleService;
        this.documentMapper = documentMapper;
    }

    public ParsedDocument parse(Long documentId) {
        RagDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        try {
            lifecycleService.moveTo(document, DocumentStatus.PARSING);
            documentMapper.updateById(document);

            ParsedDocument parsedDocument = selectParser(document).parse(document);
            if (parsedDocument.isBlank()) {
                lifecycleService.fail(document, "Parsed text is empty");
                documentMapper.updateById(document);
                throw new IllegalStateException("Parsed text is empty: " + documentId);
            }

            lifecycleService.moveTo(document, DocumentStatus.PARSED);
            documentMapper.updateById(document);
            return parsedDocument;
        } catch (RuntimeException e) {
            if (document.getStatus() != DocumentStatus.FAILED) {
                lifecycleService.fail(document, e.getMessage());
                documentMapper.updateById(document);
            }
            throw e;
        }
    }

    private DocumentParser selectParser(RagDocument document) {
        String fileType = document.getFileType();
        if (!StringUtils.hasText(fileType)) {
            throw new IllegalStateException("Document file type is empty: " + document.getId());
        }
        return parsers.stream()
                .filter(parser -> parser.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unsupported parser file type: " + fileType));
    }
}


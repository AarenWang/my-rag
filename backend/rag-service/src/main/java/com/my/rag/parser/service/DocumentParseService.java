package com.my.rag.parser.service;

import com.my.rag.collection.service.CollectionService;
import com.my.rag.document.entity.RagDocument;
import com.my.rag.document.enums.DocumentStatus;
import com.my.rag.document.repository.RagDocumentMapper;
import com.my.rag.document.service.DocumentLifecycleService;
import com.my.rag.parser.dto.ParsedDocument;
import com.my.rag.parser.strategy.DocumentParser;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DocumentParseService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParseService.class);

    private final List<DocumentParser> parsers;
    private final DocumentLifecycleService lifecycleService;
    private final RagDocumentMapper documentMapper;
    private final CollectionService collectionService;

    public DocumentParseService(
            List<DocumentParser> parsers,
            DocumentLifecycleService lifecycleService,
            RagDocumentMapper documentMapper,
            CollectionService collectionService) {
        this.parsers = parsers;
        this.lifecycleService = lifecycleService;
        this.documentMapper = documentMapper;
        this.collectionService = collectionService;
    }

    public ParsedDocument parse(Long documentId) {
        RagDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        try {
            log.info("Starting to parse documentId: {}, title: {}, fileType: {}",
                    documentId, document.getTitle(), document.getFileType());
            lifecycleService.moveTo(document, DocumentStatus.PARSING);
            updateDocumentAndCollectionCounts(document);

            ParsedDocument parsedDocument = selectParser(document).parse(document);
            log.info("Parsing completed for documentId: {}, paragraphs: {}", 
                    documentId, parsedDocument.paragraphs().size());
            
            if (parsedDocument.isBlank()) {
                lifecycleService.fail(document, "Parsed text is empty");
                updateDocumentAndCollectionCounts(document);
                throw new IllegalStateException("Parsed text is empty: " + documentId);
            }

            if (!parsedDocument.paragraphs().isEmpty()) {
                log.info("First paragraph (preview): {}",
                        parsedDocument.paragraphs().get(0).length() > 200
                            ? parsedDocument.paragraphs().get(0).substring(0, 200) + "..."
                            : parsedDocument.paragraphs().get(0));
            }

            lifecycleService.moveTo(document, DocumentStatus.PARSED);
            updateDocumentAndCollectionCounts(document);
            return parsedDocument;
        } catch (RuntimeException e) {
            log.error("Parsing failed for documentId: {}", documentId, e);
            if (document.getStatus() != DocumentStatus.FAILED) {
                lifecycleService.fail(document, e.getMessage());
                updateDocumentAndCollectionCounts(document);
            }
            throw e;
        }
    }

    private void updateDocumentAndCollectionCounts(RagDocument document) {
        documentMapper.updateById(document);
        if (document.getCollectionId() != null) {
            try {
                collectionService.updateCollectionCounts(document.getCollectionId());
            } catch (Exception e) {
                log.warn("Failed to update collection counts for collectionId: {}",
                    document.getCollectionId(), e);
            }
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


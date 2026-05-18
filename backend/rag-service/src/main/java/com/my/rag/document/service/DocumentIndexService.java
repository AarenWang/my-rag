package com.my.rag.document.service;

import com.my.rag.chunk.service.ChunkService;
import com.my.rag.document.entity.RagDocument;
import com.my.rag.document.enums.DocumentStatus;
import com.my.rag.document.repository.RagDocumentMapper;
import com.my.rag.parser.dto.Chapter;
import com.my.rag.parser.dto.ParsedDocument;
import com.my.rag.parser.service.ChapterRecognitionService;
import com.my.rag.parser.service.DocumentParseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentIndexService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexService.class);

    private final RagDocumentMapper documentMapper;
    private final DocumentLifecycleService lifecycleService;
    private final DocumentParseService parseService;
    private final ChapterRecognitionService chapterService;
    private final ChunkService chunkService;

    public DocumentIndexService(
            RagDocumentMapper documentMapper,
            DocumentLifecycleService lifecycleService,
            DocumentParseService parseService,
            ChapterRecognitionService chapterService,
            ChunkService chunkService) {
        this.documentMapper = documentMapper;
        this.lifecycleService = lifecycleService;
        this.parseService = parseService;
        this.chapterService = chapterService;
        this.chunkService = chunkService;
    }

    public void indexDocument(Long documentId, IndexProgressListener progressListener) {
        log.info("Starting index process for documentId: {}", documentId);

        RagDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            log.error("Document not found: {}", documentId);
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        try {
            progressListener.update("PARSING", 10, "Parsing document text", null);
            log.info("Step 1/3: Parsing document...");
            ParsedDocument parsedDocument = parseService.parse(documentId);
            log.info("Parsing completed, paragraphs: {}", parsedDocument.paragraphs().size());
            progressListener.update("PARSED", 40, "Document parsed", null);
            document = documentMapper.selectById(documentId);

            progressListener.update("CHAPTERING", 45, "Recognizing chapters", null);
            log.info("Step 2/3: Recognizing chapters...");
            var chapters = chapterService.recognizeChapters(parsedDocument.paragraphs());
            log.info("Chapter recognition completed, chapters: {}", chapters.size());
            progressListener.update("CHAPTERED", 55, "Chapters recognized: " + chapters.size(), null);

            progressListener.update("CHUNKING", 60, "Creating chunks", null);
            log.info("Step 3/3: Creating chunks...");
            lifecycleService.moveTo(document, DocumentStatus.CHUNKING);
            documentMapper.updateById(document);

            int chunkCount = chunkService.createChunks(documentId, parsedDocument, chapters);
            log.info("Chunk creation completed, chunks: {}", chunkCount);
            progressListener.update("CHUNKED", 95, "Chunks created: " + chunkCount, chunkCount);

            lifecycleService.moveTo(document, DocumentStatus.CHUNKED);
            documentMapper.updateById(document);
            progressListener.update("CHUNKED", 100, "Index process completed", chunkCount);

            log.info("Index process completed successfully for documentId: {}", documentId);

        } catch (Exception e) {
            log.error("Index process failed for documentId: {}", documentId, e);
            if (document.getStatus() != DocumentStatus.FAILED) {
                lifecycleService.fail(document, e.getMessage());
                documentMapper.updateById(document);
            }
            throw e;
        }
    }
}

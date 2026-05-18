package com.my.rag.parser.strategy;

import com.my.rag.document.entity.RagDocument;
import com.my.rag.parser.dto.ParsedDocument;
import com.my.rag.parser.service.TextCleaningService;
import java.util.List;

public abstract class AbstractTextParser implements DocumentParser {

    private final TextCleaningService textCleaningService;

    protected AbstractTextParser(TextCleaningService textCleaningService) {
        this.textCleaningService = textCleaningService;
    }

    protected ParsedDocument buildParsedDocument(RagDocument document, String rawText) {
        List<String> paragraphs = textCleaningService.toParagraphs(rawText);
        return new ParsedDocument(document.getId(), document.getTitle(), null, paragraphs);
    }
}

package com.my.rag.parser.strategy;

import com.my.rag.document.entity.RagDocument;
import com.my.rag.parser.dto.ParsedDocument;
import com.my.rag.parser.service.TextCleaningService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TxtDocumentParser extends AbstractTextParser {

    private static final Set<String> SUPPORTED_TYPES = Set.of("txt");

    public TxtDocumentParser(TextCleaningService textCleaningService) {
        super(textCleaningService);
    }

    @Override
    public boolean supports(String fileType) {
        return SUPPORTED_TYPES.contains(fileType);
    }

    @Override
    public ParsedDocument parse(RagDocument document) {
        try {
            String rawText = Files.readString(Path.of(document.getSourcePath()), StandardCharsets.UTF_8);
            return buildParsedDocument(document, rawText);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse TXT document: " + document.getId(), e);
        }
    }
}


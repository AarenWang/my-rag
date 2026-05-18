package com.my.rag.parser.strategy;

import com.my.rag.document.entity.RagDocument;
import com.my.rag.parser.dto.ParsedDocument;
import com.my.rag.parser.service.TextCleaningService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MarkdownDocumentParser extends AbstractTextParser {

    private static final Set<String> SUPPORTED_TYPES = Set.of("md", "markdown");
    private static final Pattern IMAGE = Pattern.compile("!\\[[^]]*]\\([^)]*\\)");
    private static final Pattern LINK = Pattern.compile("\\[([^]]+)]\\([^)]*\\)");
    private static final Pattern FENCE = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern INLINE_MARKER = Pattern.compile("[*_`>#-]+");

    public MarkdownDocumentParser(TextCleaningService textCleaningService) {
        super(textCleaningService);
    }

    @Override
    public boolean supports(String fileType) {
        return SUPPORTED_TYPES.contains(fileType);
    }

    @Override
    public ParsedDocument parse(RagDocument document) {
        try {
            String markdown = Files.readString(Path.of(document.getSourcePath()), StandardCharsets.UTF_8);
            String plainText = toPlainText(markdown);
            return buildParsedDocument(document, plainText);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Markdown document: " + document.getId(), e);
        }
    }

    private String toPlainText(String markdown) {
        String text = FENCE.matcher(markdown).replaceAll("\n");
        text = IMAGE.matcher(text).replaceAll("");
        text = LINK.matcher(text).replaceAll("$1");
        return INLINE_MARKER.matcher(text).replaceAll("");
    }
}


package com.my.rag.parser.strategy;

import com.my.rag.config.RagProperties;
import com.my.rag.document.entity.RagDocument;
import com.my.rag.parser.dto.ParsedDocument;
import com.my.rag.parser.service.TextCleaningService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TikaDocumentParser extends AbstractTextParser {

    private static final Logger log = LoggerFactory.getLogger(TikaDocumentParser.class);
    private static final Set<String> SUPPORTED_TYPES = Set.of("epub", "pdf");
    private static final Pattern SCRIPT_OR_STYLE = Pattern.compile("(?is)<(script|style)[^>]*>.*?</\\1>");
    private static final Pattern TAG = Pattern.compile("(?s)<[^>]+>");
    private static final Pattern BLANK_LINES = Pattern.compile("\\n{3,}");

    private final Tika tika = new Tika();
    private final int maxExtractChars;

    public TikaDocumentParser(TextCleaningService textCleaningService, RagProperties ragProperties) {
        super(textCleaningService);
        this.maxExtractChars = ragProperties.getIndex().getTikaMaxExtractChars();
        tika.setMaxStringLength(maxExtractChars);
    }

    @Override
    public boolean supports(String fileType) {
        return SUPPORTED_TYPES.contains(fileType);
    }

    @Override
    public ParsedDocument parse(RagDocument document) {
        try (InputStream inputStream = Files.newInputStream(Path.of(document.getSourcePath()))) {
            return buildParsedDocument(document, tika.parseToString(inputStream));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read document: " + document.getId(), e);
        } catch (Exception e) {
            if ("epub".equalsIgnoreCase(document.getFileType())) {
                log.warn("Tika failed to parse EPUB, using tolerant fallback, documentId: {}", document.getId(), e);
                return parseEpubFallback(document);
            }
            throw new IllegalStateException("Failed to parse document with Tika: " + document.getId(), e);
        }
    }

    private ParsedDocument parseEpubFallback(RagDocument document) {
        StringBuilder text = new StringBuilder(Math.min(maxExtractChars, 1024 * 1024));
        Path sourcePath = Path.of(document.getSourcePath());

        try (ZipFile zipFile = new ZipFile(sourcePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements() && text.length() < maxExtractChars) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !isHtmlEntry(entry.getName())) {
                    continue;
                }
                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    appendWithLimit(text, toPlainText(html));
                    text.append("\n\n");
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse EPUB fallback: " + document.getId(), e);
        }

        if (text.isEmpty()) {
            throw new IllegalStateException("EPUB fallback extracted no text: " + document.getId());
        }
        return buildParsedDocument(document, text.toString());
    }

    private boolean isHtmlEntry(String name) {
        String lowerName = name.toLowerCase();
        return lowerName.endsWith(".xhtml") || lowerName.endsWith(".html") || lowerName.endsWith(".htm");
    }

    private String toPlainText(String html) {
        String text = SCRIPT_OR_STYLE.matcher(html).replaceAll("\n");
        text = text.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("(?i)</p\\s*>", "\n");
        text = text.replaceAll("(?i)</h[1-6]\\s*>", "\n");
        text = TAG.matcher(text).replaceAll("");
        text = decodeCommonEntities(text);
        return BLANK_LINES.matcher(text).replaceAll("\n\n");
    }

    private String decodeCommonEntities(String text) {
        return text
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    private void appendWithLimit(StringBuilder target, String value) {
        int remaining = maxExtractChars - target.length();
        if (remaining <= 0 || value == null || value.isBlank()) {
            return;
        }
        target.append(value, 0, Math.min(value.length(), remaining));
    }
}

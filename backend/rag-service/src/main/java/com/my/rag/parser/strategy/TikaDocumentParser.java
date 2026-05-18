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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
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
    private static final Set<String> HTML_EXTENSIONS = Set.of(".xhtml", ".html", ".htm");

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
        int filesProcessed = 0;
        List<String> allEntries = new ArrayList<>();
        List<String> htmlEntries = new ArrayList<>();
        List<String> otherEntries = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(sourcePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String name = entry.getName();
                    allEntries.add(name);
                    if (isHtmlEntry(name)) {
                        htmlEntries.add(name);
                    } else {
                        otherEntries.add(name);
                    }
                }
            }
            
            log.info("EPUB contains {} files total, documentId: {}", allEntries.size(), document.getId());
            log.info("  - HTML/XHTML files: {}", htmlEntries.size());
            log.info("  - Other files: {}", otherEntries.size());
            
            if (htmlEntries.size() > 0) {
                log.info("First 10 HTML entries: {}", htmlEntries.subList(0, Math.min(htmlEntries.size(), 10)));
            }
            if (otherEntries.size() > 0) {
                log.info("First 10 other entries: {}", otherEntries.subList(0, Math.min(otherEntries.size(), 10)));
            }
            
            for (String entryName : htmlEntries) {
                if (text.length() >= maxExtractChars) {
                    log.info("Reached max extract chars limit, stopping, documentId: {}", document.getId());
                    break;
                }
                
                ZipEntry entry = zipFile.getEntry(entryName);
                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    log.debug("Parsing HTML entry: {}, size: {} chars", entryName, html.length());
                    
                    String plainText = toPlainTextRobust(html);
                    
                    if (plainText != null && !plainText.isBlank()) {
                        appendWithLimit(text, plainText);
                        text.append("\n\n");
                        filesProcessed++;
                        log.debug("  Extracted {} chars from {}", plainText.length(), entryName);
                    } else {
                        log.debug("  No text extracted from {}", entryName);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse HTML entry: {}, documentId: {}", entryName, document.getId(), e);
                }
            }
            
            if (text.length() < maxExtractChars / 2 && otherEntries.size() > 0) {
                log.info("HTML extraction only got {} chars, trying other text-like files", text.length());
                for (String entryName : otherEntries) {
                    if (text.length() >= maxExtractChars) {
                        break;
                    }
                    if (isTextLikeFile(entryName)) {
                        ZipEntry entry = zipFile.getEntry(entryName);
                        try (InputStream inputStream = zipFile.getInputStream(entry)) {
                            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                            String plainText = extractTextFromContent(content);
                            if (plainText != null && !plainText.isBlank()) {
                                appendWithLimit(text, plainText);
                                text.append("\n\n");
                                filesProcessed++;
                                log.debug("Extracted {} chars from text-like file: {}", plainText.length(), entryName);
                            }
                        } catch (Exception e) {
                            log.debug("Failed to parse text-like entry: {}", entryName, e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse EPUB fallback: " + document.getId(), e);
        }

        log.info("EPUB fallback completed, processed {} files, extracted {} chars total, documentId: {}", 
                filesProcessed, text.length(), document.getId());

        if (text.isEmpty()) {
            throw new IllegalStateException("EPUB fallback extracted no text: " + document.getId());
        }
        return buildParsedDocument(document, text.toString());
    }

    private boolean isTextLikeFile(String name) {
        String lowerName = name.toLowerCase();
        return lowerName.endsWith(".txt") || 
               lowerName.endsWith(".css") || 
               lowerName.endsWith(".js") ||
               lowerName.endsWith(".opf") ||
               lowerName.endsWith(".ncx");
    }

    private String extractTextFromContent(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String text = toPlainTextRobust(content);
        if (text.length() < 20) {
            return "";
        }
        return text;
    }

    private boolean isHtmlEntry(String name) {
        String lowerName = name.toLowerCase();
        
        for (String ext : HTML_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        
        if (lowerName.contains("/xhtml/") || lowerName.contains("/html/")) {
            return true;
        }
        
        return false;
    }

    private String toPlainTextRobust(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        try {
            String text = html;
            
            text = text.replaceAll("(?is)<script[^>]*>.*?</script>", "\n");
            text = text.replaceAll("(?is)<style[^>]*>.*?</style>", "\n");
            
            text = text.replaceAll("(?i)<br\\s*/?>", "\n");
            text = text.replaceAll("(?i)</p\\s*>", "\n");
            text = text.replaceAll("(?i)</h[1-6]\\s*>", "\n");
            text = text.replaceAll("(?i)</div\\s*>", "\n");
            text = text.replaceAll("(?i)</li\\s*>", "\n");
            text = text.replaceAll("(?i)</tr\\s*>", "\n");
            text = text.replaceAll("(?i)</td\\s*>", " ");
            
            text = removeTagsRobust(text);
            
            text = decodeHtmlEntities(text);
            
            text = text.replace("\r\n", "\n").replace('\r', '\n');
            
            text = text.replaceAll("\\n[ \\t]*\\n", "\n\n");
            text = text.replaceAll("\\n{3,}", "\n\n");
            
            return text.trim();
        } catch (Exception e) {
            log.warn("Robust HTML parsing failed", e);
            return html;
        }
    }

    private String removeTagsRobust(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        
        StringBuilder result = new StringBuilder(html.length());
        int i = 0;
        int len = html.length();
        
        while (i < len) {
            char c = html.charAt(i);
            if (c == '<') {
                int end = html.indexOf('>', i);
                if (end != -1) {
                    i = end + 1;
                } else {
                    result.append(c);
                    i++;
                }
            } else {
                result.append(c);
                i++;
            }
        }
        
        return result.toString();
    }

    private String decodeHtmlEntities(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String result = text;
        
        result = result.replace("&nbsp;", " ");
        result = result.replace("&amp;", "&");
        result = result.replace("&lt;", "<");
        result = result.replace("&gt;", ">");
        result = result.replace("&quot;", "\"");
        result = result.replace("&apos;", "'");
        result = result.replace("&copy;", "©");
        result = result.replace("&reg;", "®");
        result = result.replace("&trade;", "™");
        result = result.replace("&mdash;", "—");
        result = result.replace("&ndash;", "–");
        result = result.replace("&hellip;", "…");
        
        return result;
    }

    private void appendWithLimit(StringBuilder target, String value) {
        int remaining = maxExtractChars - target.length();
        if (remaining <= 0 || value == null || value.isBlank()) {
            return;
        }
        target.append(value, 0, Math.min(value.length(), remaining));
    }
}

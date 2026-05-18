package com.my.rag.parser.strategy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EpubParserDebugTest {

    private static final Logger log = LoggerFactory.getLogger(EpubParserDebugTest.class);
    private static final Set<String> HTML_EXTENSIONS = Set.of(".xhtml", ".html", ".htm");
    private static final int MAX_EXTRACT_CHARS = 10 * 1024 * 1024;

    public static void main(String[] args) throws Exception {
        String epubPath = "C:\\Users\\wangr\\Downloads\\黄仁勋：英伟达之芯 (【美】斯蒂芬·威特) .epub";
        Path path = Paths.get(epubPath);
        
        if (!path.toFile().exists()) {
            log.error("Test file not found at: {}", epubPath);
            return;
        }

        log.info("=" .repeat(80));
        log.info("Starting EPUB Debug Parser");
        log.info("=" .repeat(80));
        log.info("EPUB file: {}", epubPath);
        log.info("");

        StringBuilder text = new StringBuilder(Math.min(MAX_EXTRACT_CHARS, 1024 * 1024));
        int filesProcessed = 0;
        List<String> allEntries = new ArrayList<>();
        List<String> htmlEntries = new ArrayList<>();
        List<String> otherEntries = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            
            log.info("Step 1: Listing all files in EPUB...");
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
            
            log.info("  Total files in EPUB: {}", allEntries.size());
            log.info("  - HTML/XHTML files: {}", htmlEntries.size());
            log.info("  - Other files: {}", otherEntries.size());
            log.info("");
            
            if (htmlEntries.size() > 0) {
                log.info("Step 2: HTML/XHTML files (first 20):");
                for (int i = 0; i < Math.min(htmlEntries.size(), 20); i++) {
                    log.info("  [{}] {}", i + 1, htmlEntries.get(i));
                }
                if (htmlEntries.size() > 20) {
                    log.info("  ... and {} more", htmlEntries.size() - 20);
                }
            }
            log.info("");
            
            if (otherEntries.size() > 0) {
                log.info("Step 3: Other files (first 20):");
                for (int i = 0; i < Math.min(otherEntries.size(), 20); i++) {
                    log.info("  [{}] {}", i + 1, otherEntries.get(i));
                }
                if (otherEntries.size() > 20) {
                    log.info("  ... and {} more", otherEntries.size() - 20);
                }
                log.info("");
            }
            
            log.info("Step 3.5: Reading EPUB metadata files...");
            for (String entryName : otherEntries) {
                if (entryName.endsWith("content.opf") || entryName.endsWith("toc.ncx")) {
                    ZipEntry entry = zipFile.getEntry(entryName);
                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
                        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        log.info("  === {} ===", entryName);
                        log.info("{}", content);
                        log.info("  === END OF {} ===", entryName);
                    } catch (Exception e) {
                        log.error("  Failed to read {}: {}", entryName, e.getMessage(), e);
                    }
                    log.info("");
                }
            }
            log.info("");
            
            log.info("Step 4: Parsing HTML files...");
            for (int i = 0; i < htmlEntries.size(); i++) {
                String entryName = htmlEntries.get(i);
                if (text.length() >= MAX_EXTRACT_CHARS) {
                    log.info("  Reached max extract chars limit ({}), stopping", MAX_EXTRACT_CHARS);
                    break;
                }
                
                ZipEntry entry = zipFile.getEntry(entryName);
                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    
                    String plainText = toPlainTextRobust(html);
                    
                    if (plainText != null && !plainText.isBlank()) {
                        int beforeLen = text.length();
                        appendWithLimit(text, plainText);
                        text.append("\n\n");
                        int extracted = text.length() - beforeLen - 2;
                        filesProcessed++;
                        
                        if (i % 10 == 0 || i < 10 || i > htmlEntries.size() - 10) {
                            log.info("  ✓ [{}/{}] {} - extracted {} chars", 
                                    i + 1, htmlEntries.size(), entryName, extracted);
                        }
                    }
                } catch (Exception e) {
                    log.warn("  ✗ [{}/{}] {} - FAILED: {}", 
                            i + 1, htmlEntries.size(), entryName, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed to parse EPUB", e);
            throw e;
        }

        log.info("");
        log.info("=" .repeat(80));
        log.info("EPUB Parsing Summary");
        log.info("=" .repeat(80));
        log.info("Total files processed: {}", filesProcessed);
        log.info("Total characters extracted: {}", text.length());
        log.info("");
        
        List<String> paragraphs = splitIntoParagraphs(text.toString());
        log.info("Number of paragraphs: {}", paragraphs.size());
        log.info("");
        
        if (!paragraphs.isEmpty()) {
            log.info("First 5 paragraphs:");
            for (int i = 0; i < Math.min(paragraphs.size(), 5); i++) {
                String p = paragraphs.get(i);
                log.info("  [{}] ({} chars) {}", 
                        i + 1, p.length(), 
                        p.length() > 150 ? p.substring(0, 150).replace("\n", " ") + "..." : p.replace("\n", " "));
            }
            log.info("");
            
            log.info("Last 5 paragraphs:");
            int start = Math.max(0, paragraphs.size() - 5);
            for (int i = start; i < paragraphs.size(); i++) {
                String p = paragraphs.get(i);
                log.info("  [{}] ({} chars) {}", 
                        i + 1, p.length(), 
                        p.length() > 150 ? p.substring(0, 150).replace("\n", " ") + "..." : p.replace("\n", " "));
            }
        }
        
        log.info("");
        log.info("=" .repeat(80));
        log.info("Done!");
        log.info("=" .repeat(80));
    }

    private static boolean isHtmlEntry(String name) {
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

    private static String toPlainTextRobust(String html) {
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

    private static String removeTagsRobust(String html) {
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

    private static String decodeHtmlEntities(String text) {
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

    private static void appendWithLimit(StringBuilder target, String value) {
        int remaining = MAX_EXTRACT_CHARS - target.length();
        if (remaining <= 0 || value == null || value.isBlank()) {
            return;
        }
        target.append(value, 0, Math.min(value.length(), remaining));
    }

    private static List<String> splitIntoParagraphs(String text) {
        String[] split = text.split("\\n{2,}");
        List<String> paragraphs = new ArrayList<>();
        for (String s : split) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed);
            }
        }
        return paragraphs;
    }
}

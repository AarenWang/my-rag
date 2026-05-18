package com.my.rag.parser.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TextCleaningService {

    private static final Pattern MULTIPLE_SPACES = Pattern.compile("[\\t\\x0B\\f\\r ]+");
    private static final Pattern PAGE_NUMBER = Pattern.compile("^[-—\\s]*\\d+[-—\\s]*$");
    private static final Pattern CATALOG_LINE =
            Pattern.compile("^(目录|目\\s*录|contents?)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CATALOG_ENTRY =
            Pattern.compile("^.{1,80}(\\.{2,}|…{2,}|\\s{2,})\\s*\\d{1,5}$");

    public String normalize(String rawText) {
        return String.join("\n\n", toParagraphs(rawText));
    }

    public List<String> toParagraphs(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return List.of();
        }

        String normalized = rawText.replace('\u3000', ' ').replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\\n");
        List<String> paragraphs = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            String cleanedLine = cleanLine(line);
            if (!StringUtils.hasText(cleanedLine)) {
                flushParagraph(current, paragraphs, seen);
                continue;
            }
            if (isNoiseLine(cleanedLine)) {
                continue;
            }

            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(cleanedLine);
        }

        flushParagraph(current, paragraphs, seen);
        return paragraphs;
    }

    private String cleanLine(String line) {
        return MULTIPLE_SPACES.matcher(line.strip()).replaceAll(" ");
    }

    private boolean isNoiseLine(String line) {
        return PAGE_NUMBER.matcher(line).matches()
                || CATALOG_LINE.matcher(line).matches()
                || CATALOG_ENTRY.matcher(line).matches();
    }

    private void flushParagraph(StringBuilder current, List<String> paragraphs, Set<String> seen) {
        if (current.length() == 0) {
            return;
        }

        String paragraph = current.toString().strip();
        current.setLength(0);
        if (!StringUtils.hasText(paragraph)) {
            return;
        }

        String fingerprint = paragraph.replaceAll("\\s+", "");
        if (fingerprint.length() < 2 || !seen.add(fingerprint)) {
            return;
        }
        paragraphs.add(paragraph);
    }
}


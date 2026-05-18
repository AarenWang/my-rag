package com.my.rag.parser.dto;

import java.util.List;

public record ParsedDocument(Long documentId, String title, String text, List<String> paragraphs) {

    public boolean isBlank() {
        return paragraphs == null || paragraphs.isEmpty();
    }
}

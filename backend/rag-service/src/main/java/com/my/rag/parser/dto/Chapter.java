package com.my.rag.parser.dto;

import java.util.List;

public record Chapter(
        String title,
        int startParagraph,
        int endParagraph,
        List<String> contentParagraphs
) {}

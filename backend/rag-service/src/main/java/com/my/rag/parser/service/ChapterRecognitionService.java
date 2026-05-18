package com.my.rag.parser.service;

import com.my.rag.parser.dto.Chapter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ChapterRecognitionService {

    private static final String DEFAULT_CHAPTER_TITLE = "正文";

    private static final List<Pattern> CHAPTER_PATTERNS = List.of(
            Pattern.compile("^\\s*第[零一二三四五六七八九十百千万0-9]+章.*$"),
            Pattern.compile("^\\s*第[零一二三四五六七八九十百千万0-9]+节.*$"),
            Pattern.compile("^\\s*[零一二三四五六七八九十百千万]+[、.　\\s].*$"),
            Pattern.compile("^\\s*\\d+[.、.　\\s].*$"),
            Pattern.compile("^\\s*Chapter\\s+\\d+.*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*Section\\s+\\d+.*$", Pattern.CASE_INSENSITIVE)
    );

    public List<Chapter> recognizeChapters(List<String> paragraphs) {
        List<Chapter> chapters = new ArrayList<>();

        if (paragraphs == null || paragraphs.isEmpty()) {
            return chapters;
        }

        int currentStart = 0;
        String currentTitle = DEFAULT_CHAPTER_TITLE;

        for (int i = 0; i < paragraphs.size(); i++) {
            String paragraph = paragraphs.get(i);
            String matchedTitle = matchChapterTitle(paragraph);

            if (matchedTitle != null) {
                if (i > currentStart) {
                    chapters.add(createChapter(
                            currentTitle,
                            currentStart,
                            i - 1,
                            paragraphs.subList(currentStart, i)
                    ));
                }
                currentStart = i;
                currentTitle = matchedTitle;
            }
        }

        if (currentStart < paragraphs.size()) {
            chapters.add(createChapter(
                    currentTitle,
                    currentStart,
                    paragraphs.size() - 1,
                    paragraphs.subList(currentStart, paragraphs.size())
            ));
        }

        return chapters;
    }

    private String matchChapterTitle(String paragraph) {
        if (paragraph == null || paragraph.isBlank()) {
            return null;
        }
        String trimmed = paragraph.trim();
        for (Pattern pattern : CHAPTER_PATTERNS) {
            if (pattern.matcher(trimmed).matches()) {
                return trimmed;
            }
        }
        return null;
    }

    private Chapter createChapter(String title, int start, int end, List<String> contentParagraphs) {
        return new Chapter(title, start, end, new ArrayList<>(contentParagraphs));
    }
}

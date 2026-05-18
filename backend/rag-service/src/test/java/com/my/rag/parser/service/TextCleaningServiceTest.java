package com.my.rag.parser.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextCleaningServiceTest {

    private final TextCleaningService textCleaningService = new TextCleaningService();

    @Test
    void shouldCleanNoiseAndDeduplicateParagraphs() {
        String rawText =
                """
                目录
                第一章 测试上传........1

                第一章 测试上传

                这是第一段。

                这是第一段。
                2

                这是第二段。
                """;

        List<String> paragraphs = textCleaningService.toParagraphs(rawText);

        assertThat(paragraphs).containsExactly("第一章 测试上传", "这是第一段。", "这是第二段。");
        assertThat(textCleaningService.normalize(rawText)).contains("这是第二段。");
    }

    @Test
    void shouldReturnEmptyParagraphsForBlankText() {
        assertThat(textCleaningService.toParagraphs(" \n \t ")).isEmpty();
    }
}

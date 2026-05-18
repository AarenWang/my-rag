package com.my.rag.retrieval.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.my.rag.config.RagProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class KeywordQueryServiceTest {

    private final KeywordQueryService keywordQueryService = new KeywordQueryService(new RagProperties());

    @Test
    void shouldPreserveConfigToken() {
        List<String> queries = keywordQueryService.generate("RAG_EMBEDDING_PRICE_PER_1K_TOKENS 是在哪里设置的？");

        assertThat(queries).isNotEmpty();
        assertThat(String.join(" ", queries)).contains("RAG_EMBEDDING_PRICE_PER_1K_TOKENS");
        assertThat(queries).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void shouldGenerateQueriesForMixedEnglishAndChineseQuestion() {
        List<String> queries = keywordQueryService.generate("Memory 和 Context Engineering 有什么区别？");

        assertThat(queries).isNotEmpty();
        assertThat(String.join(" ", queries)).contains("Memory", "Context", "Engineering");
        assertThat(String.join(" ", queries)).contains("区别");
        assertThat(queries).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void shouldPreserveChapterAndNumericCondition() {
        List<String> queries = keywordQueryService.generate("第七章提到的 1024 维 embedding 是什么？");

        assertThat(String.join(" ", queries)).contains("第七章", "1024 维", "embedding");
        assertThat(queries).hasSizeLessThanOrEqualTo(3);
    }
}

package com.my.rag.embedding.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.my.rag.RagApplication;
import com.my.rag.embedding.dto.EmbeddingInput;
import com.my.rag.embedding.dto.EmbeddingRequest;
import com.my.rag.embedding.dto.EmbeddingResponse;
import com.my.rag.embedding.dto.EmbeddingVector;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = RagApplication.class)
class EmbeddingClientIntegrationTest {

    @Autowired
    private EmbeddingClient embeddingClient;

    @Test
    void shouldEmbedSingleText() {
        EmbeddingRequest request = new EmbeddingRequest(
                "text-embedding-v4",
                List.of(new EmbeddingInput(1L, "你好，这是一个测试文本。"))
        );

        EmbeddingResponse response = embeddingClient.embed(request);

        assertThat(response).isNotNull();
        assertThat(response.model()).isNotBlank();
        assertThat(response.dimension()).isPositive();
        assertThat(response.vectors()).hasSize(1);

        EmbeddingVector vector = response.vectors().get(0);
        assertThat(vector.chunkId()).isEqualTo(1L);
        assertThat(vector.vector()).isNotNull();
        assertThat(vector.vector()).hasSize(response.dimension());
    }

    @Test
    void shouldEmbedMultipleTexts() {
        EmbeddingRequest request = new EmbeddingRequest(
                "text-embedding-v4",
                List.of(
                        new EmbeddingInput(1L, "第一个测试文本。"),
                        new EmbeddingInput(2L, "第二个测试文本。"),
                        new EmbeddingInput(3L, "第三个测试文本。")
                )
        );

        EmbeddingResponse response = embeddingClient.embed(request);

        assertThat(response).isNotNull();
        assertThat(response.vectors()).hasSize(3);
        assertThat(response.vectors().get(0).chunkId()).isEqualTo(1L);
        assertThat(response.vectors().get(1).chunkId()).isEqualTo(2L);
        assertThat(response.vectors().get(2).chunkId()).isEqualTo(3L);
    }

    @Test
    void shouldEmbedChineseText() {
        EmbeddingRequest request = new EmbeddingRequest(
                "text-embedding-v4",
                List.of(
                        new EmbeddingInput(1L, "这是一段中文文本，用于测试 Embedding API 的功能。"),
                        new EmbeddingInput(2L, "我们希望能够准确地向量化这段内容。")
                )
        );

        EmbeddingResponse response = embeddingClient.embed(request);

        assertThat(response).isNotNull();
        assertThat(response.vectors()).hasSize(2);

        for (EmbeddingVector vector : response.vectors()) {
            assertThat(vector.vector()).isNotNull();
            assertThat(vector.vector()).hasSize(response.dimension());
        }
    }
}

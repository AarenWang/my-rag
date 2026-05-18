package com.my.rag.embedding.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.rag.config.RagProperties;
import com.my.rag.embedding.dto.EmbeddingInput;
import com.my.rag.embedding.dto.EmbeddingRequest;
import com.my.rag.embedding.dto.EmbeddingResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SimpleEmbeddingTest {

    @Test
    void testEmbeddingClient() {
        RagProperties ragProperties = new RagProperties();
        RagProperties.Model model = ragProperties.getModel();
        model.setEmbeddingProvider("dashscope");
        model.setEmbeddingBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        model.setEmbeddingApiKey("your-api-key-here");
        model.setEmbeddingModel("text-embedding-v4");
        model.setEmbeddingDimension(1024);
        model.setEmbeddingBatchSize(10);
        model.setEmbeddingRequestTimeoutSeconds(60);

        ObjectMapper objectMapper = new ObjectMapper();
        EmbeddingClient client = new OpenAiCompatibleEmbeddingClient(ragProperties, objectMapper);

        EmbeddingRequest request = new EmbeddingRequest(
                "text-embedding-v4",
                List.of(new EmbeddingInput(1L, "你好，这是一个测试文本。"))
        );

        System.out.println("=== 开始测试 Embedding API ===");
        System.out.println("请求: " + request);

        try {
            EmbeddingResponse response = client.embed(request);
            System.out.println("响应: " + response);
            System.out.println("=== 测试成功！===");
            System.out.println("模型: " + response.model());
            System.out.println("维度: " + response.dimension());
            System.out.println("向量数量: " + response.vectors().size());
            System.out.println("第一个向量长度: " + response.vectors().get(0).vector().size());
        } catch (Exception e) {
            System.err.println("=== 测试失败！===");
            e.printStackTrace();
        }
    }
}

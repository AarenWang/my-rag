package com.my.rag.chat.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.rag.chat.dto.LlmChatRequest;
import com.my.rag.chat.dto.LlmChatResponse;
import com.my.rag.chat.dto.LlmMessage;
import com.my.rag.config.RagProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SimpleLlmTest {

    @Test
    void testLlmChat() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        LlmClient client = new OpenAiCompatibleLlmClient(
                createRagProperties(),
                objectMapper
        );

        LlmChatRequest request = new LlmChatRequest(
                "deepseek-v4-pro",
                List.of(
                        LlmMessage.system("你是一个友好的助手。"),
                        LlmMessage.user("你好，请简单介绍一下自己。")
                ),
                0.2
        );

        System.out.println("=== 开始测试 LLM 调用 ===");
        System.out.println("请求: " + request);

        try {
            LlmChatResponse response = client.chat(request);
            System.out.println("响应: " + response);
            System.out.println("=== 测试成功！===");
            System.out.println("内容: " + response.content());
            System.out.println("完成原因: " + response.finishReason());
            System.out.println("Prompt Tokens: " + response.promptTokens());
            System.out.println("Completion Tokens: " + response.completionTokens());
        } catch (Exception e) {
            System.err.println("=== 测试失败！===");
            e.printStackTrace();
        }
    }

    private static RagProperties createRagProperties() {
        return new RagProperties() {
            @Override
            public Model getModel() {
                Model model = new Model();
                model.setChatBaseUrl("https://api.deepseek.com");
                model.setChatApiKey("sk-39da74d37b7349c2bbb2e36931eb322a");
                model.setChatModel("deepseek-v4-pro");
                model.setChatTemperature(0.2);
                model.setChatRequestTimeoutSeconds(60);
                return model;
            }
        };
    }
}

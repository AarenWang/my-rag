package com.my.rag.chat.client;

import com.my.rag.RagApplication;
import com.my.rag.chat.dto.LlmChatRequest;
import com.my.rag.chat.dto.LlmChatResponse;
import com.my.rag.chat.dto.LlmMessage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = RagApplication.class)
class LlmClientIntegrationTest {

    @Autowired
    private LlmClient llmClient;

    @Test
    void testLlmChat() {
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
            LlmChatResponse response = llmClient.chat(request);
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
}
